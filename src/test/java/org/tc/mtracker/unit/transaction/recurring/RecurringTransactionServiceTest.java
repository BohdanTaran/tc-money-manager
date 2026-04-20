package org.tc.mtracker.unit.transaction.recurring;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.tc.mtracker.account.Account;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.category.enums.CategoryStatus;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.support.factory.EntityTestFactory;
import org.tc.mtracker.transaction.Transaction;
import org.tc.mtracker.transaction.TransactionService;
import org.tc.mtracker.transaction.TransactionValidationService;
import org.tc.mtracker.transaction.recurring.RecurringTransaction;
import org.tc.mtracker.transaction.recurring.RecurringTransactionRepository;
import org.tc.mtracker.transaction.recurring.RecurringTransactionService;
import org.tc.mtracker.transaction.recurring.dto.RecurringTransactionCreateRequestDTO;
import org.tc.mtracker.transaction.recurring.dto.RecurringTransactionMapper;
import org.tc.mtracker.transaction.recurring.dto.RecurringTransactionResponseDTO;
import org.tc.mtracker.transaction.recurring.enums.IntervalUnit;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class RecurringTransactionServiceTest {

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private RecurringTransactionMapper recurringTransactionMapper;

    @Mock
    private UserService userService;

    @Mock
    private TransactionValidationService transactionValidationService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private RecurringTransactionService recurringTransactionService;

    @Test
    void shouldCreateRecurringTransactionForTodayAndExecuteFirstOccurrenceImmediately() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account account = EntityTestFactory.account(1L, user, BigDecimal.ZERO);
        Category category = EntityTestFactory.category(4L, user, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        RecurringTransactionCreateRequestDTO requestDTO = new RecurringTransactionCreateRequestDTO(
                new BigDecimal("2000.00"),
                TransactionType.INCOME,
                4L,
                LocalDate.of(2026, 4, 17),
                "Salary",
                null,
                IntervalUnit.MONTHLY
        );
        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                .user(user)
                .type(requestDTO.type())
                .amount(requestDTO.amount())
                .description(requestDTO.description())
                .startDate(requestDTO.date())
                .intervalUnit(requestDTO.intervalUnit())
                .build();
        RecurringTransaction savedRecurringTransaction = RecurringTransaction.builder()
                .id(10L)
                .user(user)
                .account(account)
                .category(category)
                .type(requestDTO.type())
                .amount(requestDTO.amount())
                .description(requestDTO.description())
                .startDate(requestDTO.date())
                .nextExecutionDate(LocalDate.of(2026, 5, 17))
                .intervalUnit(requestDTO.intervalUnit())
                .build();
        RecurringTransactionResponseDTO responseDTO = new RecurringTransactionResponseDTO(
                10L,
                1L,
                requestDTO.amount(),
                null,
                requestDTO.description(),
                requestDTO.type(),
                requestDTO.date(),
                LocalDate.of(2026, 5, 17),
                requestDTO.intervalUnit(),
                null,
                null
        );

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(transactionValidationService.resolveAccount(user, requestDTO.accountId())).thenReturn(account);
        when(transactionValidationService.resolveActiveCategory(requestDTO.categoryId(), user)).thenReturn(category);
        when(transactionValidationService.today()).thenReturn(LocalDate.of(2026, 4, 17));
        when(recurringTransactionMapper.toEntity(requestDTO, user)).thenReturn(recurringTransaction);
        when(recurringTransactionRepository.save(recurringTransaction)).thenReturn(savedRecurringTransaction);
        when(recurringTransactionMapper.toDto(savedRecurringTransaction)).thenReturn(responseDTO);

        RecurringTransactionResponseDTO result = recurringTransactionService.createRecurringTransaction(authentication, requestDTO);

        assertThat(result).isEqualTo(responseDTO);
        assertThat(recurringTransaction.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 5, 17));
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionService).createAutomatedTransaction(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getDate()).isEqualTo(LocalDate.of(2026, 4, 17));
        assertThat(transactionCaptor.getValue().getAccount()).isEqualTo(account);
        assertThat(transactionCaptor.getValue().getCategory()).isEqualTo(category);
    }

    @Test
    void shouldCreateRecurringTransactionForFutureWithoutImmediateExecution() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account account = EntityTestFactory.account(1L, user, BigDecimal.ZERO);
        Category category = EntityTestFactory.category(4L, user, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        RecurringTransactionCreateRequestDTO requestDTO = new RecurringTransactionCreateRequestDTO(
                new BigDecimal("2000.00"),
                TransactionType.INCOME,
                4L,
                LocalDate.of(2026, 5, 1),
                "Salary",
                null,
                IntervalUnit.MONTHLY
        );
        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                .user(user)
                .type(requestDTO.type())
                .amount(requestDTO.amount())
                .description(requestDTO.description())
                .startDate(requestDTO.date())
                .intervalUnit(requestDTO.intervalUnit())
                .build();
        RecurringTransaction savedRecurringTransaction = RecurringTransaction.builder()
                .id(10L)
                .user(user)
                .account(account)
                .category(category)
                .type(requestDTO.type())
                .amount(requestDTO.amount())
                .description(requestDTO.description())
                .startDate(requestDTO.date())
                .nextExecutionDate(requestDTO.date())
                .intervalUnit(requestDTO.intervalUnit())
                .build();

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(transactionValidationService.resolveAccount(user, requestDTO.accountId())).thenReturn(account);
        when(transactionValidationService.resolveActiveCategory(requestDTO.categoryId(), user)).thenReturn(category);
        when(transactionValidationService.today()).thenReturn(LocalDate.of(2026, 4, 17));
        when(recurringTransactionMapper.toEntity(requestDTO, user)).thenReturn(recurringTransaction);
        when(recurringTransactionRepository.save(recurringTransaction)).thenReturn(savedRecurringTransaction);
        when(recurringTransactionMapper.toDto(savedRecurringTransaction)).thenReturn(new RecurringTransactionResponseDTO(
                10L,
                1L,
                requestDTO.amount(),
                null,
                requestDTO.description(),
                requestDTO.type(),
                requestDTO.date(),
                requestDTO.date(),
                requestDTO.intervalUnit(),
                null,
                null
        ));

        recurringTransactionService.createRecurringTransaction(authentication, requestDTO);

        assertThat(recurringTransaction.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        verify(transactionService, never()).createAutomatedTransaction(any(Transaction.class));
    }
}
