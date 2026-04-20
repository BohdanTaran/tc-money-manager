package org.tc.mtracker.transaction.recurring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
class RecurringTransactionScheduler {

    private final Clock clock;
    private final RecurringTransactionExecutionService recurringTransactionExecutionService;

    @Scheduled(cron = "0 0 * * * *")
    public void executeRecurringTransactions() {
        LocalDate executionDate = LocalDate.now(clock);
        log.debug("Recurring transaction scheduler triggered for executionDate={}", executionDate);
        recurringTransactionExecutionService.executeDueTransactions(executionDate);
    }
}
