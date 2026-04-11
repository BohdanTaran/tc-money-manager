package org.tc.mtracker.transaction.recurring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tc.mtracker.transaction.Transaction;
import org.tc.mtracker.transaction.TransactionService;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionExecutionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final RecurringTransactionService recurringTransactionService;
    private final TransactionService transactionService;

    @Transactional
    void executeDueTransactions(LocalDate executionDate) {
        List<RecurringTransaction> dueRecurringTransactions =
                recurringTransactionRepository.findDueTransactions(executionDate);

        for (RecurringTransaction recurringTransaction : dueRecurringTransactions) {
            executeRecurringTransaction(recurringTransaction, executionDate);
        }

        if (!dueRecurringTransactions.isEmpty()) {
            log.info("Executed {} recurring transaction template(s) up to {}", dueRecurringTransactions.size(), executionDate);
        }
    }

    private void executeRecurringTransaction(RecurringTransaction recurringTransaction, LocalDate executionDate) {
        while (!recurringTransaction.getNextExecutionDate().isAfter(executionDate)) {
            transactionService.createAutomatedTransaction(toTransaction(recurringTransaction));
            recurringTransaction.setNextExecutionDate(
                    recurringTransactionService.nextExecutionDateAfter(
                            recurringTransaction.getNextExecutionDate(),
                            recurringTransaction.getIntervalUnit()
                    )
            );
        }
    }

    private static Transaction toTransaction(RecurringTransaction recurringTransaction) {
        return Transaction.builder()
                .user(recurringTransaction.getUser())
                .account(recurringTransaction.getAccount())
                .category(recurringTransaction.getCategory())
                .type(recurringTransaction.getType())
                .amount(recurringTransaction.getAmount())
                .description(recurringTransaction.getDescription())
                .date(recurringTransaction.getNextExecutionDate())
                .build();
    }
}
