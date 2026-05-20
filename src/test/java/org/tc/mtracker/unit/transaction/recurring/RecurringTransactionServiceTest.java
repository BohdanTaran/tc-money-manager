package org.tc.mtracker.unit.transaction.recurring;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tc.mtracker.account.Account;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.category.enums.CategoryStatus;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.support.factory.EntityTestFactory;
import org.tc.mtracker.transaction.Transaction;
import org.tc.mtracker.transaction.TransactionMutationService;
import org.tc.mtracker.transaction.TransactionRepository;
import org.tc.mtracker.transaction.TransactionValidationService;
import org.tc.mtracker.transaction.dto.TransactionCreateRequestDTO;
import org.tc.mtracker.transaction.dto.TransactionUpdateRequestDTO;
import org.tc.mtracker.transaction.recurring.RecurringTransaction;
import org.tc.mtracker.transaction.recurring.RecurringTransactionRepository;
import org.tc.mtracker.transaction.recurring.RecurringTransactionService;
import org.tc.mtracker.transaction.recurring.dto.RecurringTransactionMapper;
import org.tc.mtracker.transaction.recurring.enums.IntervalUnit;
import org.tc.mtracker.transaction.recurring.enums.RecurringTransactionChangeScope;
import org.tc.mtracker.user.User;
import org.tc.mtracker.utils.exceptions.RecurringTransactionScopeException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class RecurringTransactionServiceTest {

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private RecurringTransactionMapper recurringTransactionMapper;

    @Mock
    private TransactionValidationService transactionValidationService;

    @Mock
    private TransactionMutationService transactionMutationService;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private RecurringTransactionService recurringTransactionService;

    @Test
    void shouldCreateRecurringTransactionForTodayAndScheduleNextExecution() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account account = EntityTestFactory.account(1L, user, BigDecimal.ZERO);
        Category category = EntityTestFactory.category(4L, user, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        TransactionCreateRequestDTO requestDTO = new TransactionCreateRequestDTO(
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
        when(transactionValidationService.today()).thenReturn(LocalDate.of(2026, 4, 17));
        when(recurringTransactionMapper.toEntity(requestDTO, user, category, account)).thenReturn(recurringTransaction);
        when(recurringTransactionRepository.save(recurringTransaction)).thenReturn(savedRecurringTransaction);

        RecurringTransaction result = recurringTransactionService.createRecurringTransaction(user, account, category, requestDTO);

        assertThat(result).isEqualTo(savedRecurringTransaction);
        assertThat(recurringTransaction.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 5, 17));
        verify(transactionMutationService, never()).persistTransaction(any(Transaction.class));
    }

    @Test
    void shouldCreateRecurringTransactionForFutureWithoutImmediateExecution() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account account = EntityTestFactory.account(1L, user, BigDecimal.ZERO);
        Category category = EntityTestFactory.category(4L, user, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        TransactionCreateRequestDTO requestDTO = new TransactionCreateRequestDTO(
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

        when(transactionValidationService.today()).thenReturn(LocalDate.of(2026, 4, 17));
        when(recurringTransactionMapper.toEntity(requestDTO, user, category, account)).thenReturn(recurringTransaction);
        when(recurringTransactionRepository.save(recurringTransaction)).thenReturn(savedRecurringTransaction);

        RecurringTransaction result = recurringTransactionService.createRecurringTransaction(user, account, category, requestDTO);

        assertThat(result).isEqualTo(savedRecurringTransaction);
        assertThat(recurringTransaction.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        verify(transactionMutationService, never()).persistTransaction(any(Transaction.class));
    }

    @Test
    void shouldUpdateSelectedOccurrenceAndRecurringRuleWhenScopeIsCurrentAndFuture() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account account = EntityTestFactory.account(1L, user, new BigDecimal("200.00"));
        Category salaryCategory = EntityTestFactory.category(4L, user, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        Category bonusCategory = EntityTestFactory.category(5L, user, "Bonus", TransactionType.INCOME, CategoryStatus.ACTIVE);
        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                .id(10L)
                .user(user)
                .account(account)
                .category(salaryCategory)
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("100.00"))
                .description("Salary")
                .startDate(LocalDate.of(2026, 4, 1))
                .nextExecutionDate(LocalDate.of(2026, 6, 1))
                .intervalUnit(IntervalUnit.MONTHLY)
                .build();
        Transaction selectedOccurrence = EntityTestFactory.transaction(
                11L,
                user,
                account,
                salaryCategory,
                TransactionType.INCOME,
                new BigDecimal("100.00"),
                LocalDate.of(2026, 4, 1)
        );
        selectedOccurrence.setRecurringTransaction(recurringTransaction);
        TransactionUpdateRequestDTO updateDto = new TransactionUpdateRequestDTO(
                new BigDecimal("150.00"),
                TransactionType.INCOME,
                5L,
                LocalDate.of(2026, 4, 2),
                "Updated salary",
                1L,
                IntervalUnit.YEARLY,
                RecurringTransactionChangeScope.THIS_AND_FUTURE
        );

        when(transactionValidationService.today()).thenReturn(LocalDate.of(2026, 4, 17));
        doAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            TransactionUpdateRequestDTO requestDTO = invocation.getArgument(1);
            Account targetAccount = invocation.getArgument(2);
            Category category = invocation.getArgument(3);
            transaction.setAmount(requestDTO.amount());
            transaction.setType(requestDTO.type());
            transaction.setDescription(requestDTO.description());
            transaction.setDate(requestDTO.date());
            transaction.setAccount(targetAccount);
            transaction.setCategory(category);
            return null;
        }).when(transactionMutationService).updateTransactionValues(selectedOccurrence, updateDto, account, bonusCategory);

        recurringTransactionService.updateCurrentAndFutureOccurrences(
                selectedOccurrence,
                updateDto,
                account,
                bonusCategory,
                user
        );

        assertThat(selectedOccurrence.getDate()).isEqualTo(LocalDate.of(2026, 4, 2));
        assertThat(selectedOccurrence.getAmount()).isEqualByComparingTo("150.00");
        assertThat(selectedOccurrence.getCategory()).isEqualTo(bonusCategory);
        assertThat(recurringTransaction.getStartDate()).isEqualTo(LocalDate.of(2026, 4, 2));
        assertThat(recurringTransaction.getNextExecutionDate()).isEqualTo(LocalDate.of(2027, 4, 2));
        assertThat(recurringTransaction.getIntervalUnit()).isEqualTo(IntervalUnit.YEARLY);
        assertThat(recurringTransaction.getAmount()).isEqualByComparingTo("150.00");
        assertThat(recurringTransaction.getCategory()).isEqualTo(bonusCategory);
        assertThat(recurringTransaction.getDescription()).isEqualTo("Updated salary");
    }

    @Test
    void shouldRejectCurrentAndFutureScopeForTransactionWithoutRecurringRule() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account account = EntityTestFactory.account(1L, user, BigDecimal.ZERO);
        Category category = EntityTestFactory.category(4L, user, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        Transaction transaction = EntityTestFactory.transaction(
                11L,
                user,
                account,
                category,
                TransactionType.INCOME,
                new BigDecimal("100.00"),
                LocalDate.of(2026, 4, 1)
        );

        assertThatThrownBy(() -> recurringTransactionService.deleteCurrentAndFutureOccurrences(transaction, user))
                .isInstanceOf(RecurringTransactionScopeException.class);

        verifyNoInteractions(transactionMutationService);
    }

    @Test
    void shouldConvertRecurringToOneTimeAndDeleteFutureOccurrences() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account account = EntityTestFactory.account(1L, user, BigDecimal.ZERO);
        Category category = EntityTestFactory.category(4L, user, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                .id(10L)
                .user(user)
                .account(account)
                .category(category)
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("100.00"))
                .description("Salary")
                .startDate(LocalDate.of(2026, 4, 1))
                .nextExecutionDate(LocalDate.of(2026, 5, 1))
                .intervalUnit(IntervalUnit.MONTHLY)
                .build();
        Transaction selectedOccurrence = EntityTestFactory.transaction(
                11L,
                user,
                account,
                category,
                TransactionType.INCOME,
                new BigDecimal("100.00"),
                LocalDate.of(2026, 4, 2)
        );
        selectedOccurrence.setRecurringTransaction(recurringTransaction);
        Transaction futureOccurrence = EntityTestFactory.transaction(
                12L,
                user,
                account,
                category,
                TransactionType.INCOME,
                new BigDecimal("100.00"),
                LocalDate.of(2026, 5, 2)
        );
        futureOccurrence.setRecurringTransaction(recurringTransaction);
        TransactionUpdateRequestDTO updateDto = new TransactionUpdateRequestDTO(
                new BigDecimal("150.00"),
                TransactionType.INCOME,
                4L,
                LocalDate.of(2026, 4, 2),
                "Updated salary",
                1L,
                IntervalUnit.ONCE,
                RecurringTransactionChangeScope.THIS_AND_FUTURE
        );

        when(transactionRepository.findAllByRecurringTransactionAndDateGreaterThanEqualOrderByDateAscIdAsc(
                recurringTransaction,
                LocalDate.of(2026, 4, 2)
        )).thenReturn(java.util.List.of(selectedOccurrence, futureOccurrence));

        recurringTransactionService.convertRecurringToOneTime(
                selectedOccurrence,
                updateDto,
                account,
                category,
                user
        );

        verify(transactionMutationService).updateTransactionValues(selectedOccurrence, updateDto, account, category);
        verify(transactionMutationService).deleteSingleTransaction(futureOccurrence);
        verify(recurringTransactionRepository).delete(recurringTransaction);
        assertThat(selectedOccurrence.getRecurringTransaction()).isNull();
    }

    @Test
    void shouldDeleteCurrentAndFutureOccurrences() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account account = EntityTestFactory.account(1L, user, BigDecimal.ZERO);
        Category category = EntityTestFactory.category(4L, user, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                .id(10L)
                .user(user)
                .account(account)
                .category(category)
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("100.00"))
                .description("Salary")
                .startDate(LocalDate.of(2026, 4, 1))
                .nextExecutionDate(LocalDate.of(2026, 5, 1))
                .intervalUnit(IntervalUnit.MONTHLY)
                .build();
        Transaction selectedOccurrence = EntityTestFactory.transaction(
                11L,
                user,
                account,
                category,
                TransactionType.INCOME,
                new BigDecimal("100.00"),
                LocalDate.of(2026, 4, 2)
        );
        selectedOccurrence.setRecurringTransaction(recurringTransaction);
        Transaction futureOccurrence = EntityTestFactory.transaction(
                12L,
                user,
                account,
                category,
                TransactionType.INCOME,
                new BigDecimal("100.00"),
                LocalDate.of(2026, 5, 2)
        );
        futureOccurrence.setRecurringTransaction(recurringTransaction);

        when(transactionRepository.findAllByRecurringTransactionAndDateGreaterThanEqualOrderByDateAscIdAsc(
                recurringTransaction,
                LocalDate.of(2026, 4, 2)
        )).thenReturn(java.util.List.of(selectedOccurrence, futureOccurrence));

        recurringTransactionService.deleteCurrentAndFutureOccurrences(selectedOccurrence, user);

        verify(transactionMutationService).deleteSingleTransaction(futureOccurrence);
        verify(transactionMutationService).deleteSingleTransaction(selectedOccurrence);
        verify(recurringTransactionRepository).delete(recurringTransaction);
    }
}
