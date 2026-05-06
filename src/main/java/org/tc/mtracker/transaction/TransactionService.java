package org.tc.mtracker.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.tc.mtracker.transaction.recurring.RecurringTransactionService;
import org.tc.mtracker.transaction.recurring.enums.RecurringTransactionChangeScope;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserService;
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

    @Transactional
    public TransactionResponseDTO createTransaction(Authentication auth, TransactionCreateRequestDTO createRequestDTO, List<MultipartFile> receipts) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        transactionValidationService.validateOneTimeTransactionDate(createRequestDTO.date(), user);
        Account account = transactionValidationService.resolveAccount(user, createRequestDTO.accountId());
        Transaction transaction = transactionMapper.toEntity(createRequestDTO, user);
        Category category = transactionValidationService.resolveActiveCategory(createRequestDTO.categoryId(), user);
        transactionValidationService.validateTransactionType(createRequestDTO.type(), category, user);

        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setCategory(category);
        transactionMutationService.addReceiptsToTransaction(receipts, transaction);

        Transaction saved = transactionMutationService.persistTransaction(transaction);
        log.info("Transaction created userId={} transactionId={} accountId={} amount={} type={}",
                user.getId(), saved.getId(), account.getId(), saved.getAmount(), saved.getType());

        return transactionMutationService.toResponseDto(saved);
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
            TransactionCreateRequestDTO updateRequestDTO,
            RecurringTransactionChangeScope recurringScope
    ) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        transactionValidationService.validateOneTimeTransactionDate(updateRequestDTO.date(), user);
        Transaction transaction = findActiveOwnedTransaction(transactionId, user);

        Account targetAccount = transactionValidationService.resolveAccount(user, updateRequestDTO.accountId());
        Category category = transactionValidationService.resolveActiveCategory(updateRequestDTO.categoryId(), user);

        transactionValidationService.validateTransactionType(updateRequestDTO.type(), category, user);

        if (recurringScope == RecurringTransactionChangeScope.THIS_AND_FUTURE) {
            recurringTransactionService.updateCurrentAndFutureOccurrences(
                    transaction,
                    updateRequestDTO,
                    targetAccount,
                    category,
                    user
            );
        } else {
            transactionMutationService.updateTransactionValues(transaction, updateRequestDTO, targetAccount, category);
        }

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction updated userId={} transactionId={} accountId={} amount={} type={}",
                user.getId(), saved.getId(), targetAccount.getId(), saved.getAmount(), saved.getType());
        return transactionMutationService.toResponseDto(saved);
    }

    @Transactional
    public void deleteTransaction(Long transactionId, Authentication auth, RecurringTransactionChangeScope recurringScope) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        Transaction transaction = findActiveOwnedTransaction(transactionId, user);

        if (recurringScope == RecurringTransactionChangeScope.THIS_AND_FUTURE) {
            recurringTransactionService.deleteCurrentAndFutureOccurrences(transaction, user);
        } else {
            transactionMutationService.deleteSingleTransaction(transaction);
        }

        log.info("Transaction deleted userId={} transactionId={}", user.getId(), transactionId);
    }

    private Transaction findActiveOwnedTransaction(Long transactionId, User user) {
        return transactionRepository.findActiveByIdAndUser(transactionId, user)
                .orElseThrow(() -> {
                    log.warn("Transaction not found userId={} transactionId={}", user.getId(), transactionId);
                    return new TransactionNotFoundException("Transaction with id %d not found".formatted(transactionId));
                });
    }

}
