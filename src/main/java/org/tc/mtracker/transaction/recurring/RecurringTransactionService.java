package org.tc.mtracker.transaction.recurring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tc.mtracker.account.Account;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.transaction.Transaction;
import org.tc.mtracker.transaction.TransactionMutationService;
import org.tc.mtracker.transaction.TransactionRepository;
import org.tc.mtracker.transaction.TransactionValidationService;
import org.tc.mtracker.transaction.dto.TransactionCreateRequestDTO;
import org.tc.mtracker.transaction.dto.TransactionUpdateRequestDTO;
import org.tc.mtracker.transaction.recurring.dto.RecurringTransactionMapper;
import org.tc.mtracker.transaction.recurring.dto.RecurringTransactionResponseDTO;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserService;
import org.tc.mtracker.utils.exceptions.RecurringTransactionNotFoundException;
import org.tc.mtracker.utils.exceptions.RecurringTransactionScopeException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final RecurringTransactionMapper recurringTransactionMapper;
    private final UserService userService;
    private final TransactionValidationService transactionValidationService;
    private final TransactionMutationService transactionMutationService;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<RecurringTransactionResponseDTO> getRecurringTransactions(Authentication auth) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        log.debug("Loading recurring transactions for userId={}", user.getId());
        return recurringTransactionMapper.toDtos(recurringTransactionRepository.findAllByUserOrderBySchedule(user.getId()));
    }

    @Transactional(readOnly = true)
    public RecurringTransactionResponseDTO getRecurringTransactionById(Long recurringTransactionId, Authentication auth) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        log.debug("Loading recurring transaction for userId={} recurringTransactionId={}", user.getId(), recurringTransactionId);
        return recurringTransactionMapper.toDto(findOwnedRecurringTransaction(recurringTransactionId, user));
    }

    public RecurringTransaction createRecurringTransaction(
            User user,
            Account account,
            Category category,
            TransactionCreateRequestDTO requestDTO
    ) {
        transactionValidationService.validateRecurringStartDate(requestDTO.date(), user);
        LocalDate today = transactionValidationService.today();
        transactionValidationService.validateTransactionType(requestDTO.type(), category, user);
        RecurringTransaction recurringTransaction = recurringTransactionMapper
                .toEntity(requestDTO, category, account);

        boolean startToday = requestDTO.date().isEqual(today);
        recurringTransaction.setNextExecutionDate(startToday
                ? recurringTransaction.nextExecutionDateAfter(requestDTO.date())
                : requestDTO.date());

        RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);

        log.info("Recurring transaction created userId={} recurringTransactionId={} startDate={} firstExecutionDate={} intervalUnit={}",
                user.getId(), saved.getId(), saved.getStartDate(), saved.getNextExecutionDate(), saved.getIntervalUnit());
        return saved;
    }

    public void deleteRecurringTransaction(Long recurringTransactionId, Authentication auth) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        RecurringTransaction recurringTransaction = findOwnedRecurringTransaction(recurringTransactionId, user);
        recurringTransactionRepository.delete(recurringTransaction);
        log.info("Recurring transaction deleted userId={} recurringTransactionId={}", user.getId(), recurringTransactionId);
    }

    public void updateCurrentAndFutureOccurrences(
            Transaction transaction,
            TransactionUpdateRequestDTO updateRequestDTO,
            Account targetAccount,
            Category category,
            User user
    ) {
        RecurringTransaction recurringTransaction = getRecurringTransaction(transaction, user);

        transactionMutationService.updateTransactionValues(transaction, updateRequestDTO, targetAccount, category);

        recurringTransaction.setAccount(targetAccount);
        recurringTransaction.setCategory(category);
        recurringTransaction.setAmount(updateRequestDTO.amount());
        recurringTransaction.setDescription(updateRequestDTO.description());
        recurringTransaction.setStartDate(updateRequestDTO.date());
        recurringTransaction.setNextExecutionDate(
                recurringTransaction.nextExecutionDateAfter(updateRequestDTO.date(), transactionValidationService.today())
        );

        recurringTransactionRepository.save(recurringTransaction);
        log.info("Recurring transaction updated userId={}, recurringTransactionId ={}",
                user.getId(), recurringTransaction.getId());
    }

    public void deleteCurrentAndFutureOccurrences(Transaction transaction, User user) {
        RecurringTransaction recurringTransaction = getRecurringTransaction(transaction, user);

        List<Transaction> futureOccurrences = transactionRepository
                .findAllByRecurringTransactionAndDateGreaterThanEqualOrderByDateAscIdAsc(
                        recurringTransaction,
                        transaction.getDate()
                );
        for (Transaction futureOccurrence : futureOccurrences) {
            if (!futureOccurrence.getId().equals(transaction.getId())) {
                transactionMutationService.deleteSingleTransaction(futureOccurrence);
            }
        }

        transactionMutationService.deleteSingleTransaction(transaction);
        recurringTransactionRepository.delete(recurringTransaction);
    }

    public void createAutomatedTransaction(Transaction transaction) {
        Transaction saved = transactionMutationService.persistTransaction(transaction);
        log.info("Automated transaction created userId={} transactionId={} accountId={} amount={} type={} date={}",
                saved.getAccount().getUser().getId(),
                saved.getId(),
                saved.getAccount().getId(),
                saved.getAmount(),
                saved.getType(),
                saved.getDate());
    }

    private RecurringTransaction findOwnedRecurringTransaction(Long recurringTransactionId, User user) {
        return recurringTransactionRepository.findByIdAndUserId(recurringTransactionId, user.getId())
                .orElseThrow(() -> {
                    log.warn("Recurring transaction not found userId={} recurringTransactionId={}", user.getId(), recurringTransactionId);
                    return new RecurringTransactionNotFoundException(
                            "Recurring transaction with id %d not found".formatted(recurringTransactionId)
                    );
                });
    }

    private RecurringTransaction getRecurringTransaction(Transaction transaction, User user) {
        RecurringTransaction recurringTransaction = transaction.getRecurringTransaction();
        if (recurringTransaction == null) {
            log.warn("Recurring scope rejected userId={} transactionId={} reason=not_recurring",
                    user.getId(), transaction.getId());
            throw new RecurringTransactionScopeException(
                    "Transaction with id %d is not linked to a recurring transaction.".formatted(transaction.getId())
            );
        }

        return recurringTransaction;
    }
}
