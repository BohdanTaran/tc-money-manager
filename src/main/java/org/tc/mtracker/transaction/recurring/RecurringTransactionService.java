package org.tc.mtracker.transaction.recurring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tc.mtracker.account.Account;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.transaction.Transaction;
import org.tc.mtracker.transaction.TransactionService;
import org.tc.mtracker.transaction.TransactionValidationService;
import org.tc.mtracker.transaction.recurring.dto.RecurringTransactionCreateRequestDTO;
import org.tc.mtracker.transaction.recurring.dto.RecurringTransactionMapper;
import org.tc.mtracker.transaction.recurring.dto.RecurringTransactionResponseDTO;
import org.tc.mtracker.transaction.recurring.enums.IntervalUnit;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserService;
import org.tc.mtracker.utils.exceptions.RecurringTransactionNotFoundException;

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
    private final TransactionService transactionService;

    @Transactional(readOnly = true)
    public List<RecurringTransactionResponseDTO> getRecurringTransactions(Authentication auth) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        log.debug("Loading recurring transactions for userId={}", user.getId());
        return recurringTransactionMapper.toDtos(recurringTransactionRepository.findAllByUserOrderBySchedule(user));
    }

    @Transactional(readOnly = true)
    public RecurringTransactionResponseDTO getRecurringTransactionById(Long recurringTransactionId, Authentication auth) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        log.debug("Loading recurring transaction for userId={} recurringTransactionId={}", user.getId(), recurringTransactionId);
        return recurringTransactionMapper.toDto(findOwnedRecurringTransaction(recurringTransactionId, user));
    }

    public RecurringTransactionResponseDTO createRecurringTransaction(
            Authentication auth,
            RecurringTransactionCreateRequestDTO requestDTO
    ) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        transactionValidationService.validateRecurringStartDate(requestDTO.date(), user);
        LocalDate today = transactionValidationService.today();

        Account account = transactionValidationService.resolveAccount(user, requestDTO.accountId());
        Category category = transactionValidationService.resolveActiveCategory(requestDTO.categoryId(), user);
        transactionValidationService.validateTransactionType(requestDTO.type(), category, user);

        RecurringTransaction recurringTransaction = recurringTransactionMapper.toEntity(requestDTO, user);
        recurringTransaction.setAccount(account);
        recurringTransaction.setCategory(category);

        boolean startToday = requestDTO.date().isEqual(today);
        recurringTransaction.setNextExecutionDate(startToday
                ? nextExecutionDateAfter(requestDTO.date(), requestDTO.intervalUnit())
                : requestDTO.date());

        RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);

        if (startToday) {
            transactionService.createAutomatedTransaction(toTransaction(saved, requestDTO.date()));
            log.info("Recurring transaction created and executed immediately userId={} recurringTransactionId={} startDate={} intervalUnit={}",
                    user.getId(), saved.getId(), saved.getStartDate(), saved.getIntervalUnit());
        } else {
            log.info("Recurring transaction created userId={} recurringTransactionId={} startDate={} firstExecutionDate={} intervalUnit={}",
                    user.getId(), saved.getId(), saved.getStartDate(), saved.getNextExecutionDate(), saved.getIntervalUnit());
        }

        return recurringTransactionMapper.toDto(saved);
    }

    public void deleteRecurringTransaction(Long recurringTransactionId, Authentication auth) {
        User user = userService.getCurrentAuthenticatedUser(auth);
        RecurringTransaction recurringTransaction = findOwnedRecurringTransaction(recurringTransactionId, user);
        recurringTransactionRepository.delete(recurringTransaction);
        log.info("Recurring transaction deleted userId={} recurringTransactionId={}", user.getId(), recurringTransactionId);
    }

    public LocalDate nextExecutionDateAfter(LocalDate baseDate, IntervalUnit intervalUnit) {
        return switch (intervalUnit) {
            case MONTHLY -> baseDate.plusMonths(1);
            case YEARLY -> baseDate.plusYears(1);
        };
    }

    private RecurringTransaction findOwnedRecurringTransaction(Long recurringTransactionId, User user) {
        return recurringTransactionRepository.findByIdAndUser(recurringTransactionId, user)
                .orElseThrow(() -> {
                    log.warn("Recurring transaction not found userId={} recurringTransactionId={}", user.getId(), recurringTransactionId);
                    return new RecurringTransactionNotFoundException(
                            "Recurring transaction with id %d not found".formatted(recurringTransactionId)
                    );
                });
    }

    static Transaction toTransaction(RecurringTransaction recurringTransaction, LocalDate transactionDate) {
        return Transaction.builder()
                .user(recurringTransaction.getUser())
                .account(recurringTransaction.getAccount())
                .category(recurringTransaction.getCategory())
                .type(recurringTransaction.getType())
                .amount(recurringTransaction.getAmount())
                .description(recurringTransaction.getDescription())
                .date(transactionDate)
                .build();
    }
}
