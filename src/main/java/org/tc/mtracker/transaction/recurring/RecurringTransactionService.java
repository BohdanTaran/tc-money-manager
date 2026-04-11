package org.tc.mtracker.transaction.recurring;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tc.mtracker.transaction.Transaction;
import org.tc.mtracker.transaction.recurring.enums.IntervalUnit;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;

    public RecurringTransaction createFromTransaction(Transaction transaction, IntervalUnit intervalUnit) {
        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                .user(transaction.getUser())
                .account(transaction.getAccount())
                .category(transaction.getCategory())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .startDate(transaction.getDate())
                .nextExecutionDate(nextExecutionDateAfter(transaction.getDate(), intervalUnit))
                .intervalUnit(intervalUnit)
                .build();

        return recurringTransactionRepository.save(recurringTransaction);
    }

    LocalDate nextExecutionDateAfter(LocalDate baseDate, IntervalUnit intervalUnit) {
        return switch (intervalUnit) {
            case MONTHLY -> baseDate.plusMonths(1);
            case YEARLY -> baseDate.plusYears(1);
        };
    }
}
