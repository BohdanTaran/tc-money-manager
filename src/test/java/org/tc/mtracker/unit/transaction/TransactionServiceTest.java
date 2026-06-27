package org.tc.mtracker.unit.transaction;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.tc.mtracker.account.Account;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.category.enums.CategoryStatus;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.support.factory.EntityTestFactory;
import org.tc.mtracker.transaction.*;
import org.tc.mtracker.transaction.dto.TransactionCreateRequestDTO;
import org.tc.mtracker.transaction.dto.TransactionMapper;
import org.tc.mtracker.transaction.dto.TransactionResponseDTO;
import org.tc.mtracker.transaction.dto.TransactionUpdateRequestDTO;
import org.tc.mtracker.transaction.recurring.RecurringTransaction;
import org.tc.mtracker.transaction.recurring.RecurringTransactionService;
import org.tc.mtracker.transaction.recurring.enums.IntervalUnit;
import org.tc.mtracker.transaction.recurring.enums.RecurringTransactionChangeScope;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserService;
import org.tc.mtracker.utils.exceptions.CategoryIsNotActiveException;
import org.tc.mtracker.utils.exceptions.MoneyFlowTypeMismatchException;
import org.tc.mtracker.utils.exceptions.TransactionNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private UserService userService;

    @Mock
    private TransactionValidationService transactionValidationService;

    @Mock
    private RecurringTransactionService recurringTransactionService;

    @Mock
    private TransactionMutationService transactionMutationService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransactionService transactionService;

    private static TransactionCreateRequestDTO createRequest(
            BigDecimal amount,
            TransactionType type,
            LocalDate date,
            String description
    ) {
        return new TransactionCreateRequestDTO(
                amount,
                type,
                4L,
                date,
                description,
                null,
                IntervalUnit.ONCE
        );
    }

    private static TransactionCreateRequestDTO createRequest(
            BigDecimal amount,
            LocalDate date
    ) {
        return new TransactionCreateRequestDTO(
                amount,
                TransactionType.INCOME,
                4L,
                date,
                "Salary",
                null,
                IntervalUnit.MONTHLY
        );
    }

    private static TransactionUpdateRequestDTO createUpdateRequest(
            BigDecimal amount,
            LocalDate date,
            String description,
            Long accountId,
            RecurringTransactionChangeScope recurringTransactionChangeScope
    ) {
        return new TransactionUpdateRequestDTO(
                amount,
                4L,
                date,
                description,
                accountId,
                recurringTransactionChangeScope
        );
    }

    @Test
    void shouldUseDefaultAccountAndIncreaseBalanceForOneTimeIncomeTransaction() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account defaultAccount = EntityTestFactory.account(1L, user, new BigDecimal("10.00"));
        Category category = EntityTestFactory.category(4L, user, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        TransactionCreateRequestDTO dto = createRequest(
                new BigDecimal("15.50"),
                TransactionType.INCOME,
                LocalDate.of(2026, 4, 1),
                "Salary"
        );
        Transaction transaction = EntityTestFactory.transaction(
                null,
                defaultAccount,
                category,
                dto.type(),
                dto.amount(),
                dto.date()
        );
        TransactionResponseDTO response = new TransactionResponseDTO(
                10L,
                1L,
                null,
                dto.amount(),
                null,
                dto.description(),
                dto.type(),
                List.of(),
                dto.date(),
                null,
                null
        );
        EntityTestFactory.linkDefaultAccount(user, defaultAccount);

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(transactionValidationService.resolveAccount(user, dto.accountId())).thenReturn(defaultAccount);
        when(transactionValidationService.resolveActiveCategory(dto.categoryId(), user)).thenReturn(category);
        when(transactionMapper.toEntity(dto, defaultAccount, category)).thenReturn(transaction);
        when(transactionMutationService.persistTransaction(transaction)).thenReturn(transaction);
        when(transactionMutationService.toResponseDto(transaction)).thenReturn(response);

        TransactionResponseDTO result = transactionService.createTransaction(authentication, dto, List.of());

        assertThat(result).isEqualTo(response);
        assertThat(transaction.getAccount().getUser()).isEqualTo(user);
        assertThat(transaction.getAccount()).isEqualTo(defaultAccount);
        assertThat(transaction.getCategory()).isEqualTo(category);
        verify(transactionValidationService).validateOneTimeTransactionDate(dto.date(), user);
        verify(transactionValidationService).validateTransactionType(dto.type(), category, user);
        verify(transactionValidationService).resolveAccount(user, null);
        verify(transactionMutationService).addReceiptsToTransaction(List.of(), transaction);
        verify(transactionMutationService).persistTransaction(transaction);
    }

    @Test
    void shouldUploadReceiptsAndReturnPresignedUrlsDuringCreate() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account defaultAccount = EntityTestFactory.account(1L, user, BigDecimal.ZERO);
        Category category = EntityTestFactory.category(4L, user, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        TransactionCreateRequestDTO dto = createRequest(
                new BigDecimal("15.50"),
                TransactionType.INCOME,
                LocalDate.of(2026, 4, 1),
                "Salary"
        );
        Transaction transaction = EntityTestFactory.transaction(
                null,
                null,
                null,
                dto.type(),
                dto.amount(),
                dto.date());
        MockMultipartFile receipt = new MockMultipartFile("receipts", "receipt.jpg", "image/jpeg", "receipt".getBytes());
        TransactionResponseDTO response = new TransactionResponseDTO(
                10L,
                1L,
                null,
                dto.amount(),
                null,
                dto.description(),
                dto.type(),
                List.of("https://test-bucket.local/receipt-1"),
                dto.date(),
                null,
                null
        );
        EntityTestFactory.linkDefaultAccount(user, defaultAccount);

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(transactionValidationService.resolveAccount(user, dto.accountId())).thenReturn(defaultAccount);
        when(transactionValidationService.resolveActiveCategory(dto.categoryId(), user)).thenReturn(category);
        when(transactionMapper.toEntity(dto, defaultAccount, category)).thenReturn(transaction);
        when(transactionMutationService.persistTransaction(transaction)).thenReturn(transaction);
        when(transactionMutationService.toResponseDto(transaction)).thenReturn(response);

        TransactionResponseDTO result = transactionService.createTransaction(authentication, dto, List.of(receipt));

        assertThat(result).isEqualTo(response);
        verify(transactionMutationService).addReceiptsToTransaction(List.of(receipt), transaction);
    }

    @Test
    void shouldCreateRecurringTransactionForTodayAndReturnGeneratedOccurrence() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account defaultAccount = EntityTestFactory.account(1L, user, BigDecimal.ZERO);
        Category category = EntityTestFactory.category(4L, user, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        TransactionCreateRequestDTO dto = createRequest(
                new BigDecimal("2000.00"),
                LocalDate.of(2026, 4, 17)
        );
        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                .id(10L)
                .account(defaultAccount)
                .category(category)
                .amount(dto.amount())
                .type(dto.type())
                .description(dto.description())
                .startDate(dto.date())
                .nextExecutionDate(LocalDate.of(2026, 5, 17))
                .intervalUnit(dto.intervalUnit())
                .build();
        MockMultipartFile receipt = new MockMultipartFile("receipts", "receipt.jpg", "image/jpeg", "receipt".getBytes());
        TransactionResponseDTO response = new TransactionResponseDTO(
                11L,
                1L,
                IntervalUnit.MONTHLY,
                dto.amount(),
                null,
                dto.description(),
                dto.type(),
                List.of("https://test-bucket.local/receipt-1"),
                dto.date(),
                null,
                null
        );

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(transactionValidationService.resolveAccount(user, dto.accountId())).thenReturn(defaultAccount);
        when(transactionValidationService.resolveActiveCategory(dto.categoryId(), user)).thenReturn(category);
        when(transactionValidationService.today()).thenReturn(LocalDate.of(2026, 4, 17));
        when(recurringTransactionService.createRecurringTransaction(user, defaultAccount, category, dto))
                .thenReturn(recurringTransaction);
        when(transactionMutationService.persistTransaction(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionMutationService.toResponseDto(any(Transaction.class))).thenReturn(response);

        TransactionResponseDTO result = transactionService.createTransaction(authentication, dto, List.of(receipt));

        assertThat(result).isEqualTo(response);
        verify(transactionMutationService).addReceiptsToTransaction(eq(List.of(receipt)), argThat(transaction ->
                transaction.getRecurringTransaction() == recurringTransaction
                        && transaction.getAccount() == defaultAccount
                        && transaction.getCategory() == category
                        && transaction.getDate().equals(dto.date())
        ));
        verify(transactionMutationService).persistTransaction(argThat(transaction ->
                transaction.getRecurringTransaction() == recurringTransaction
                        && transaction.getAmount().compareTo(dto.amount()) == 0
                        && transaction.getType() == dto.type()
        ));
        verify(transactionMapper, never()).toEntity(any(), any(), any());
    }

    @Test
    void shouldPropagateCategoryInactiveValidationDuringCreate() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        TransactionCreateRequestDTO dto = createRequest(
                BigDecimal.ONE,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 4, 1),
                "Expense"
        );

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        doThrow(new CategoryIsNotActiveException("Category is not active."))
                .when(transactionValidationService).resolveActiveCategory(dto.categoryId(), user);

        assertThatThrownBy(() -> transactionService.createTransaction(authentication, dto, List.of()))
                .isInstanceOf(CategoryIsNotActiveException.class);

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldPropagateTypeMismatchValidationDuringCreate() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account defaultAccount = EntityTestFactory.account(1L, user, BigDecimal.ZERO);
        Category category = EntityTestFactory.category(4L, user, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        TransactionCreateRequestDTO dto = createRequest(
                BigDecimal.ONE,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 4, 1),
                "Expense"
        );

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(transactionValidationService.resolveAccount(user, dto.accountId())).thenReturn(defaultAccount);
        when(transactionValidationService.resolveActiveCategory(dto.categoryId(), user)).thenReturn(category);
        doThrow(new MoneyFlowTypeMismatchException("Category type does not match transaction type."))
                .when(transactionValidationService).validateTransactionType(dto.type(), category, user);

        assertThatThrownBy(() -> transactionService.createTransaction(authentication, dto, List.of()))
                .isInstanceOf(MoneyFlowTypeMismatchException.class);

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRecalculateBalancesWhenUpdatingTransactionAndChangingAccount() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account sourceAccount = EntityTestFactory.account(1L, user, new BigDecimal("70.00"));
        Account targetAccount = EntityTestFactory.account(2L, user, new BigDecimal("20.00"));
        Category expenseCategory = EntityTestFactory.category(4L, user, "Groceries", TransactionType.EXPENSE, CategoryStatus.ACTIVE);
        Transaction existingTransaction = EntityTestFactory.transaction(
                9L,
                sourceAccount,
                expenseCategory,
                TransactionType.EXPENSE,
                new BigDecimal("30.00"),
                LocalDate.of(2026, 4, 1)
        );
        TransactionUpdateRequestDTO updateDto = createUpdateRequest(
                new BigDecimal("50.00"),
                LocalDate.of(2026, 4, 2),
                "Updated expense",
                2L,
                RecurringTransactionChangeScope.ONLY_THIS
        );
        TransactionResponseDTO response = new TransactionResponseDTO(
                9L,
                2L,
                null,
                updateDto.amount(),
                null,
                updateDto.description(),
                existingTransaction.getType(),
                List.of(),
                updateDto.date(),
                null,
                null
        );

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(transactionRepository.findActiveByIdAndUser(9L, user.getId())).thenReturn(Optional.of(existingTransaction));
        when(transactionValidationService.resolveAccount(user, 2L)).thenReturn(targetAccount);
        when(transactionValidationService.resolveActiveCategory(4L, user)).thenReturn(expenseCategory);
        when(transactionMutationService.toResponseDto(existingTransaction)).thenReturn(response);
        when(transactionRepository.findById(9L)).thenReturn(Optional.of(existingTransaction));

        TransactionResponseDTO result = transactionService.updateTransaction(
                9L,
                authentication,
                updateDto
        );

        assertThat(result).isEqualTo(response);
        verify(transactionMutationService).updateTransactionValues(existingTransaction, updateDto, targetAccount, expenseCategory);
        verify(transactionValidationService).validateTransactionType(existingTransaction.getType(), expenseCategory, user);
    }

    @Test
    void shouldDelegateRecurringCurrentAndFutureUpdateToRecurringService() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account account = EntityTestFactory.account(1L, user, BigDecimal.ZERO);
        Category salaryCategory = EntityTestFactory.category(4L, user, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        RecurringTransaction recurringTransaction = RecurringTransaction.builder()
                .id(10L)
                .account(account)
                .category(salaryCategory)
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("100.00"))
                .description("Salary")
                .startDate(LocalDate.of(2026, 4, 1))
                .nextExecutionDate(LocalDate.of(2026, 5, 1))
                .intervalUnit(IntervalUnit.MONTHLY)
                .build();
        Transaction transaction = EntityTestFactory.transaction(
                9L,
                account,
                salaryCategory,
                TransactionType.INCOME,
                new BigDecimal("100.00"),
                LocalDate.of(2026, 4, 1)
        );
        transaction.setRecurringTransaction(recurringTransaction);
        TransactionUpdateRequestDTO updateDto = createUpdateRequest(
                new BigDecimal("150.00"),
                LocalDate.of(2026, 4, 2),
                "Updated salary",
                1L,
                RecurringTransactionChangeScope.THIS_AND_FUTURE
        );
        TransactionResponseDTO response = new TransactionResponseDTO(
                9L,
                1L,
                null,
                updateDto.amount(),
                null,
                updateDto.description(),
                transaction.getType(),
                List.of(),
                updateDto.date(),
                null,
                null
        );

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(transactionRepository.findActiveByIdAndUser(9L, user.getId())).thenReturn(Optional.of(transaction));
        when(transactionValidationService.resolveAccount(user, 1L)).thenReturn(account);
        when(transactionValidationService.resolveActiveCategory(4L, user)).thenReturn(salaryCategory);
        when(transactionRepository.findById(9L)).thenReturn(Optional.of(transaction));
        when(transactionMutationService.toResponseDto(transaction)).thenReturn(response);

        TransactionResponseDTO result = transactionService.updateTransaction(
                9L,
                authentication,
                updateDto
        );

        assertThat(result).isEqualTo(response);
        verify(recurringTransactionService).updateCurrentAndFutureOccurrences(transaction, updateDto, account, salaryCategory, user);
        verify(transactionMutationService, never()).updateTransactionValues(any(), any(), any(), any());
    }

    @Test
    void shouldDeleteTransactionAndRollbackBalance() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account account = EntityTestFactory.account(1L, user, new BigDecimal("30.00"));
        Category category = EntityTestFactory.category(4L, user, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        Transaction transaction = EntityTestFactory.transaction(
                9L,
                account,
                category,
                TransactionType.INCOME,
                new BigDecimal("30.00"),
                LocalDate.of(2026, 4, 1)
        );
        UUID receiptId = UUID.randomUUID();
        EntityTestFactory.attachReceipts(transaction, new org.tc.mtracker.transaction.ReceiptImage(receiptId, transaction));

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(transactionRepository.findActiveByIdAndUser(9L, user.getId())).thenReturn(Optional.of(transaction));

        transactionService.deleteTransaction(9L, authentication, RecurringTransactionChangeScope.ONLY_THIS);

        verify(transactionMutationService).deleteSingleTransaction(transaction);
    }

    @Test
    void shouldDelegateRecurringCurrentAndFutureDeleteToRecurringService() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account account = EntityTestFactory.account(1L, user, new BigDecimal("30.00"));
        Category category = EntityTestFactory.category(
                4L,
                user,
                "Salary",
                TransactionType.INCOME,
                CategoryStatus.ACTIVE);
        Transaction transaction = EntityTestFactory.transaction(
                9L,
                account,
                category,
                TransactionType.INCOME,
                new BigDecimal("30.00"),
                LocalDate.of(2026, 4, 1)
        );

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(transactionRepository.findActiveByIdAndUser(9L, user.getId())).thenReturn(Optional.of(transaction));

        transactionService.deleteTransaction(
                9L,
                authentication,
                RecurringTransactionChangeScope.THIS_AND_FUTURE
        );

        verify(recurringTransactionService).deleteCurrentAndFutureOccurrences(transaction, user);
        verify(transactionMutationService, never()).deleteSingleTransaction(any(Transaction.class));
    }

    @Test
    void shouldThrowWhenOwnedTransactionIsMissing() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(transactionRepository.findActiveByIdAndUser(99L, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionById(99L, authentication))
                .isInstanceOf(TransactionNotFoundException.class);
    }
}
