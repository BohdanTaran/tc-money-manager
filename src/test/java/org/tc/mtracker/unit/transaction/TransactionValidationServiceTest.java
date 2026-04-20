package org.tc.mtracker.unit.transaction;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tc.mtracker.account.AccountRepository;
import org.tc.mtracker.category.CategoryService;
import org.tc.mtracker.support.factory.EntityTestFactory;
import org.tc.mtracker.transaction.TransactionValidationService;
import org.tc.mtracker.user.User;
import org.tc.mtracker.utils.exceptions.InvalidTransactionDateException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class TransactionValidationServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private Clock clock;

    @InjectMocks
    private TransactionValidationService transactionValidationService;

    @Test
    void shouldRejectFutureOneTimeTransactionDate() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        stubToday(LocalDate.of(2026, 4, 17));

        assertThatThrownBy(() -> transactionValidationService.validateOneTimeTransactionDate(LocalDate.of(2026, 4, 18), user))
                .isInstanceOf(InvalidTransactionDateException.class)
                .hasMessage("One-time transaction date cannot be in the future.");
    }

    @Test
    void shouldRejectPastRecurringStartDate() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        stubToday(LocalDate.of(2026, 4, 17));

        assertThatThrownBy(() -> transactionValidationService.validateRecurringStartDate(LocalDate.of(2026, 4, 16), user))
                .isInstanceOf(InvalidTransactionDateException.class)
                .hasMessage("Recurring transaction start date must be today or in the future.");
    }

    private void stubToday(LocalDate today) {
        Instant instant = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        org.mockito.Mockito.when(clock.instant()).thenReturn(instant);
        org.mockito.Mockito.when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }
}
