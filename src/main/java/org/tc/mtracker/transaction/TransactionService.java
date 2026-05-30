package org.tc.mtracker.transaction;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.account.Account;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.transaction.dto.TransactionCreateRequestDTO;
import org.tc.mtracker.transaction.dto.TransactionMapper;
import org.tc.mtracker.transaction.dto.TransactionResponseDTO;
import org.tc.mtracker.transaction.dto.TransactionUpdateRequestDTO;
import org.tc.mtracker.transaction.recurring.RecurringTransaction;
import org.tc.mtracker.transaction.recurring.RecurringTransactionService;
import org.tc.mtracker.transaction.recurring.enums.IntervalUnit;
import org.tc.mtracker.transaction.recurring.enums.RecurringTransactionChangeScope;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserService;
import org.tc.mtracker.utils.exceptions.InvalidReceiptAttachmentException;
import org.tc.mtracker.utils.exceptions.RecurringTransactionScopeException;
import org.tc.mtracker.utils.exceptions.TransactionNotFoundException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final UserService userService;
    private final TransactionValidationService transactionValidationService;
    private final RecurringTransactionService recurringTransactionService;
    private final TransactionMutationService transactionMutationService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public TransactionResponseDTO createTransaction(Authentication auth,
                                                    TransactionCreateRequestDTO createRequestDTO,
                                                    List<MultipartFile> receipts) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        Account account = transactionValidationService.resolveAccount(user, createRequestDTO.accountId());
        Category category = transactionValidationService.resolveActiveCategory(createRequestDTO.categoryId(), user);
        IntervalUnit intervalUnit = createRequestDTO.intervalUnit();

        Transaction transaction;
        if (isOneTimeTransaction(intervalUnit)) {
            transaction = createOneTimeTransaction(createRequestDTO, receipts, user, account, category);
        } else {
            transaction = createRecurringTransaction(createRequestDTO, receipts, user, account, category);
        }

        return transactionMutationService.toResponseDto(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponseDTO> getTransactions(
            Authentication auth,
            Long accountId,
            Long categoryId,
            TransactionType type,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        log.debug("Loading transactions for userId={} accountId={} categoryId={} type={} dateFrom={} dateTo={}",
                user.getId(), accountId, categoryId, type, dateFrom, dateTo);

        if (accountId != null) {
            transactionValidationService.resolveAccount(user, accountId);
        }
        if (categoryId != null) {
            transactionValidationService.resolveAccessibleCategory(categoryId, user);
        }

        return transactionRepository.findAllByUserAndFilters(user, accountId, categoryId, type, dateFrom, dateTo)
                .stream()
                .map(transactionMutationService::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponseDTO getTransactionById(Long transactionId, Authentication auth) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        Transaction transaction = findActiveOwnedTransaction(transactionId, user);
        log.debug("Transaction returned for userId={} transactionId={}", user.getId(), transactionId);
        return transactionMutationService.toResponseDto(transaction);
    }

    @Transactional
    public TransactionResponseDTO updateTransaction(
            Long transactionId,
            Authentication auth,
            TransactionUpdateRequestDTO updateRequestDTO
    ) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        Transaction transaction = findActiveOwnedTransaction(transactionId, user);
        Account targetAccount = transactionValidationService.resolveAccount(user, updateRequestDTO.accountId());
        Category category = transactionValidationService.resolveActiveCategory(updateRequestDTO.categoryId(), user);
        transactionValidationService.validateTransactionType(transaction.getType(), category, user);

        RecurringTransactionChangeScope recurringScope = updateRequestDTO.transactionChangeScope();

        if (recurringScope == RecurringTransactionChangeScope.ONLY_THIS) {
            transactionMutationService.updateTransactionValues(transaction, updateRequestDTO, targetAccount, category);
        } else if (recurringScope == RecurringTransactionChangeScope.THIS_AND_FUTURE
                && transaction.isRecurring()) {
            recurringTransactionService.updateCurrentAndFutureOccurrences(
                    transaction,
                    updateRequestDTO,
                    targetAccount,
                    category,
                    user);
        } else {
            throw new RecurringTransactionScopeException("Scope THIS_AND_FUTURE is allowed " +
                    "only for recurring transactions.");
        }

        Transaction saved = transactionRepository.saveAndFlush(transaction);
        entityManager.refresh(transaction);
        log.info("Transaction updated userId={} transactionId={} accountId={} amount={} type={} updateTime={}",
                user.getId(), saved.getId(), targetAccount.getId(), saved.getAmount(), saved.getType(), saved.getUpdatedAt());
        return transactionMutationService.toResponseDto(saved);
    }

    @Transactional
    public void deleteTransaction(Long transactionId,
                                  Authentication auth,
                                  RecurringTransactionChangeScope recurringScope) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        Transaction transaction = findActiveOwnedTransaction(transactionId, user);

        if (recurringScope == RecurringTransactionChangeScope.THIS_AND_FUTURE) {
            recurringTransactionService.deleteCurrentAndFutureOccurrences(transaction, user);
        } else {
            transactionMutationService.deleteSingleTransaction(transaction);
        }

        log.info("Transaction deleted userId={} transactionId={}", user.getId(), transactionId);
    }

    private static boolean isOneTimeTransaction(IntervalUnit intervalUnit) {
        return intervalUnit == null || intervalUnit == IntervalUnit.ONCE;
    }

    private @NonNull Transaction createOneTimeTransaction(TransactionCreateRequestDTO createRequestDTO,
                                                          List<MultipartFile> receipts,
                                                          User user,
                                                          Account account,
                                                          Category category) {
        Transaction transaction = transactionMapper.toEntity(createRequestDTO, user, account, category);
        transactionValidationService.validateTransactionType(createRequestDTO.type(), category, user);
        transactionValidationService.validateOneTimeTransactionDate(createRequestDTO.date(), user);
        transactionMutationService.addReceiptsToTransaction(receipts, transaction);

        transaction = transactionMutationService.persistTransaction(transaction);
        log.info("Transaction created userId={} transactionId={} accountId={} amount={} type={}",
                user.getId(), transaction.getId(), account.getId(), transaction.getAmount(), transaction.getType());
        return transaction;
    }

    private Transaction createRecurringTransaction(TransactionCreateRequestDTO createRequestDTO,
                                                   List<MultipartFile> receipts,
                                                   User user,
                                                   Account account,
                                                   Category category) {
        if (createRequestDTO.date().isAfter(transactionValidationService.today()) && hasReceipts(receipts)) {
            throw new InvalidReceiptAttachmentException("Cannot attach receipts to future transaction");
        }
        RecurringTransaction recurringTransaction = recurringTransactionService.createRecurringTransaction(
                user,
                account,
                category,
                createRequestDTO
        );
        Transaction transaction = recurringTransaction.toTransaction(createRequestDTO.date());

        if (createRequestDTO.date().isEqual(transactionValidationService.today())) {
            transactionMutationService.addReceiptsToTransaction(receipts, transaction);
            transaction = transactionMutationService.persistTransaction(transaction);
            log.info("Recurring transaction occurrence created userId={} transactionId={} recurringTransactionId={} accountId={} amount={} type={}",
                    user.getId(), transaction.getId(), recurringTransaction.getId(), account.getId(), transaction.getAmount(), transaction.getType());

        }
        return transaction;
    }

    private Transaction findActiveOwnedTransaction(Long transactionId, User user) {
        return transactionRepository.findActiveByIdAndUser(transactionId, user)
                .orElseThrow(() -> {
                    log.warn("Transaction not found userId={} transactionId={}", user.getId(), transactionId);
                    return new TransactionNotFoundException("Transaction with id %d not found".formatted(transactionId));
                });
    }

    private boolean hasReceipts(List<MultipartFile> receipts) {
        return receipts != null && !receipts.isEmpty();
    }

}
