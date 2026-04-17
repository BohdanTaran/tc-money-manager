package org.tc.mtracker.transaction.recurring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        int createdTransactions = 0;

        for (RecurringTransaction recurringTransaction : dueRecurringTransactions) {
            createdTransactions += executeRecurringTransaction(recurringTransaction, executionDate);
        }

        if (!dueRecurringTransactions.isEmpty()) {
            log.info("Executed {} recurring transaction template(s) and created {} transaction(s) up to {}",
                    dueRecurringTransactions.size(), createdTransactions, executionDate);
        }
    }

    private int executeRecurringTransaction(RecurringTransaction recurringTransaction, LocalDate executionDate) {
        int createdTransactions = 0;
        while (!recurringTransaction.getNextExecutionDate().isAfter(executionDate)) {
            LocalDate transactionDate = recurringTransaction.getNextExecutionDate();
            transactionService.createAutomatedTransaction(RecurringTransactionService.toTransaction(recurringTransaction, transactionDate));
            recurringTransaction.setNextExecutionDate(
                    recurringTransactionService.nextExecutionDateAfter(
                            transactionDate,
                            recurringTransaction.getIntervalUnit()
                    )
            );
            createdTransactions++;
        }
        return createdTransactions;
    }
}
