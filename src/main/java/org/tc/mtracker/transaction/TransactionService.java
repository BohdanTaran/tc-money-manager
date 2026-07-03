package org.tc.mtracker.transaction;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.account.Account;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.transaction.dto.*;
import org.tc.mtracker.transaction.recurring.RecurringTransaction;
import org.tc.mtracker.transaction.recurring.RecurringTransactionService;
import org.tc.mtracker.transaction.recurring.enums.IntervalUnit;
import org.tc.mtracker.transaction.recurring.enums.RecurringTransactionChangeScope;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserService;
import org.tc.mtracker.utils.exceptions.InvalidReceiptAttachmentException;
import org.tc.mtracker.utils.exceptions.RecurringTransactionScopeException;
import org.tc.mtracker.utils.exceptions.TransactionNotFoundException;

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
    public TransactionCursorPageResponseDTO<TransactionResponseDTO> getTransactions(
            TransactionCursorRequest request,
            Long userId
    ) {
        TransactionCursor cursor = null;
        if (request.cursor() != null && !request.cursor().isBlank()) {
            try {
                cursor = TransactionCursor.decode(request.cursor());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid cursor format: {}, ignoring cursor", request.cursor());
            }
        }

        String type = request.type() != null ? request.type().name() : null;
        String description = normalizeDescriptionSearch(request.description());

        List<Long> transactionIds = transactionRepository.findTransactionIdsWithCursor(
                userId,
                request.accountId(),
                request.categoryId(),
                type,
                description,
                request.dateFrom(),
                request.dateTo(),
                cursor != null ? cursor.getDate() : null,
                cursor != null ? cursor.getId() : null,
                request.limit() + 1
        );

        boolean hasNext = transactionIds.size() > request.limit();
        if (hasNext) {
            transactionIds = transactionIds.subList(0, request.limit());
        }

        List<Transaction> transactions = transactionRepository
                .findAllByIdInOrderByDateDescIdDesc(transactionIds);

        String nextCursor = null;
        if (hasNext && !transactions.isEmpty()) {
            Transaction lastTransaction = transactions.getLast();
            nextCursor = TransactionCursor.fromTransaction(lastTransaction).encode();
        }

        List<TransactionResponseDTO> responseData = transactions.stream()
                .map(t -> transactionMapper.toDto(
                        t, transactionMutationService.generatePresignedUrlsForReceipts(t)))
                .toList();

        return new TransactionCursorPageResponseDTO<>(
                responseData,
                nextCursor,
                hasNext,
                responseData.size()
        );
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

        transactionRepository.saveAndFlush(transaction);

        Transaction updated = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found after update"));

        log.info("Transaction updated userId={} transactionId={} accountId={} amount={} type={} updateTime={}",
                user.getId(), updated.getId(), targetAccount.getId(), updated.getAmount(), updated.getType(), updated.getUpdatedAt());
        return transactionMutationService.toResponseDto(updated);
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

    private static String normalizeDescriptionSearch(String description) {
        if (description == null) {
            return null;
        }

        String normalized = description.trim();
        if (normalized.length() < 3) {
            return null;
        }

        return normalized
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private @NonNull Transaction createOneTimeTransaction(TransactionCreateRequestDTO createRequestDTO,
                                                          List<MultipartFile> receipts,
                                                          User user,
                                                          Account account,
                                                          Category category) {
        Transaction transaction = transactionMapper.toEntity(createRequestDTO, account, category);
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
        return transactionRepository.findActiveByIdAndUser(transactionId, user.getId())
                .orElseThrow(() -> {
                    log.warn("Transaction not found userId={} transactionId={}", user.getId(), transactionId);
                    return new TransactionNotFoundException("Transaction with id %d not found".formatted(transactionId));
                });
    }

    private boolean hasReceipts(List<MultipartFile> receipts) {
        return receipts != null && !receipts.isEmpty();
    }

}
