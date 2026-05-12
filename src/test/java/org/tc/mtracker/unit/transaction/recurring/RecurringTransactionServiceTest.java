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
import org.tc.mtracker.transaction.TransactionMutationService;
import org.tc.mtracker.transaction.TransactionValidationService;
import org.tc.mtracker.transaction.dto.TransactionCreateRequestDTO;
import org.tc.mtracker.transaction.recurring.RecurringTransaction;
import org.tc.mtracker.transaction.recurring.RecurringTransactionRepository;
import org.tc.mtracker.transaction.recurring.RecurringTransactionService;
import org.tc.mtracker.transaction.recurring.dto.RecurringTransactionCreateRequestDTO;
import org.tc.mtracker.transaction.recurring.dto.RecurringTransactionMapper;
import org.tc.mtracker.transaction.recurring.dto.RecurringTransactionResponseDTO;
import org.tc.mtracker.transaction.recurring.enums.IntervalUnit;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserService;
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
    private UserService userService;

    @Mock
    private TransactionValidationService transactionValidationService;

    @Mock
    private TransactionMutationService transactionMutationService;

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
        when(transactionMutationService.persistTransaction(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringTransactionResponseDTO result = recurringTransactionService.createRecurringTransaction(authentication, requestDTO);

        assertThat(result).isEqualTo(responseDTO);
        assertThat(recurringTransaction.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 5, 17));
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionMutationService).persistTransaction(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getDate()).isEqualTo(LocalDate.of(2026, 4, 17));
        assertThat(transactionCaptor.getValue().getAccount()).isEqualTo(account);
        assertThat(transactionCaptor.getValue().getCategory()).isEqualTo(category);
        assertThat(transactionCaptor.getValue().getRecurringTransaction()).isEqualTo(savedRecurringTransaction);
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
        TransactionCreateRequestDTO updateDto = new TransactionCreateRequestDTO(
                new BigDecimal("150.00"),
                TransactionType.INCOME,
                5L,
                LocalDate.of(2026, 4, 2),
                "Updated salary",
                1L
        );

        when(transactionValidationService.today()).thenReturn(LocalDate.of(2026, 4, 17));
        doAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            TransactionCreateRequestDTO requestDTO = invocation.getArgument(1);
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
        assertThat(recurringTransaction.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 5, 2));
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
}
