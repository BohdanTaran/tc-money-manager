package org.tc.mtracker.transaction.recurring;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/internal/test")
@RequiredArgsConstructor
class TestAutomationController {
    private final RecurringTransactionScheduler scheduler;

    @PostMapping("/trigger-recurring-transactions")
    public ResponseEntity<Void> triggerSchedulerManual() {
        scheduler.executeRecurringTransactions();
        return ResponseEntity.ok().build();
    }
}
