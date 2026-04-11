package org.tc.mtracker.transaction.recurring;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
class RecurringTransactionScheduler {

    private final RecurringTransactionExecutionService recurringTransactionExecutionService;

    @Scheduled(cron = "0 0 * * * *")
    public void executeRecurringTransactions() {
        recurringTransactionExecutionService.executeDueTransactions(LocalDate.now());
    }
}
