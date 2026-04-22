package org.tc.mtracker.unit.transaction.recurring;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tc.mtracker.account.Account;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.category.enums.CategoryStatus;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.support.factory.EntityTestFactory;
import org.tc.mtracker.transaction.Transaction;
import org.tc.mtracker.transaction.TransactionService;
import org.tc.mtracker.transaction.recurring.RecurringTransaction;
import org.tc.mtracker.transaction.recurring.RecurringTransactionExecutionService;
import org.tc.mtracker.transaction.recurring.RecurringTransactionRepository;
import org.tc.mtracker.transaction.recurring.RecurringTransactionService;
import org.tc.mtracker.transaction.recurring.enums.IntervalUnit;
import org.tc.mtracker.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class RecurringTransactionExecutionServiceTest {

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private RecurringTransactionService recurringTransactionService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private RecurringTransactionExecutionService recurringTransactionExecutionService;

    private static RecurringTransaction recurringTransaction(
            User user,
            Account account,
            Category category,
            LocalDate startDate,
            LocalDate nextExecutionDate,
            IntervalUnit intervalUnit
    ) {
        return RecurringTransaction.builder()
                .id(10L)
                .user(user)
                .account(account)
                .category(category)
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("100.00"))
                .description("Salary")
                .startDate(startDate)
                .nextExecutionDate(nextExecutionDate)
                .intervalUnit(intervalUnit)
                .build();
    }

    @Test
    void shouldCreateTransactionsForAllMissedExecutionDates() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account account = EntityTestFactory.account(1L, user, BigDecimal.ZERO);
        Category category = EntityTestFactory.category(2L, user, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        RecurringTransaction recurringTransaction = recurringTransaction(
                user,
                account,
                category,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 1),
                IntervalUnit.MONTHLY
        );

        when(recurringTransactionRepository.findDueTransactions(LocalDate.of(2026, 4, 15), CategoryStatus.ACTIVE))
                .thenReturn(List.of(recurringTransaction));
        when(recurringTransactionService.nextExecutionDateAfter(any(LocalDate.class), eq(IntervalUnit.MONTHLY)))
                .thenAnswer(invocation -> invocation.<LocalDate>getArgument(0).plusMonths(1));

        recurringTransactionExecutionService.executeDueTransactions(LocalDate.of(2026, 4, 15));

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionService, times(3)).createAutomatedTransaction(transactionCaptor.capture());
        assertThat(transactionCaptor.getAllValues())
                .extracting(Transaction::getDate)
                .containsExactly(
                        LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 4, 1)
                );
        assertThat(recurringTransaction.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 5, 1));
    }

    @Test
    void shouldSkipArchivedRecurringCategoriesWhenLookingUpDueTransactions() {
        when(recurringTransactionRepository.findDueTransactions(LocalDate.of(2026, 4, 15), CategoryStatus.ACTIVE))
                .thenReturn(List.of());

        recurringTransactionExecutionService.executeDueTransactions(LocalDate.of(2026, 4, 15));

        verify(recurringTransactionRepository).findDueTransactions(LocalDate.of(2026, 4, 15), CategoryStatus.ACTIVE);
        verifyNoInteractions(transactionService);
    }
}
