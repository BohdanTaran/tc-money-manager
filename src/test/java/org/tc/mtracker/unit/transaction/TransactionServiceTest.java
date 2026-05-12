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
import org.tc.mtracker.transaction.Transaction;
import org.tc.mtracker.transaction.TransactionMutationService;
import org.tc.mtracker.transaction.TransactionRepository;
import org.tc.mtracker.transaction.TransactionService;
import org.tc.mtracker.transaction.TransactionValidationService;
import org.tc.mtracker.transaction.dto.TransactionCreateRequestDTO;
import org.tc.mtracker.transaction.dto.TransactionMapper;
import org.tc.mtracker.transaction.dto.TransactionResponseDTO;
import org.tc.mtracker.transaction.recurring.RecurringTransactionService;
import org.tc.mtracker.transaction.recurring.enums.RecurringTransactionChangeScope;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserService;
import org.tc.mtracker.utils.S3Service;
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
import static org.mockito.ArgumentMatchers.anyString;
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
    private S3Service s3Service;

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
            Long categoryId,
            LocalDate date,
            String description,
            Long accountId
    ) {
        return new TransactionCreateRequestDTO(
                amount,
                type,
                categoryId,
                date,
                description,
                accountId
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
                4L,
                LocalDate.of(2026, 4, 1),
                "Salary",
                null
        );
        Transaction transaction = EntityTestFactory.transaction(null, user, null, null, dto.type(), dto.amount(), dto.date());
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
        when(transactionMapper.toEntity(dto, user)).thenReturn(transaction);
        when(transactionMutationService.persistTransaction(transaction)).thenReturn(transaction);
        when(transactionMutationService.toResponseDto(transaction)).thenReturn(response);

        TransactionResponseDTO result = transactionService.createTransaction(authentication, dto, List.of());

        assertThat(result).isEqualTo(response);
        assertThat(transaction.getUser()).isEqualTo(user);
        assertThat(transaction.getAccount()).isEqualTo(defaultAccount);
        assertThat(transaction.getCategory()).isEqualTo(category);
        verify(transactionValidationService).validateOneTimeTransactionDate(dto.date(), user);
        verify(transactionValidationService).validateTransactionType(dto.type(), category, user);
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
                4L,
                LocalDate.of(2026, 4, 1),
                "Salary",
                null
        );
        Transaction transaction = EntityTestFactory.transaction(null, user, null, null, dto.type(), dto.amount(), dto.date());
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
        when(transactionMapper.toEntity(dto, user)).thenReturn(transaction);
        when(transactionMutationService.persistTransaction(transaction)).thenReturn(transaction);
        when(transactionMutationService.toResponseDto(transaction)).thenReturn(response);

        TransactionResponseDTO result = transactionService.createTransaction(authentication, dto, List.of(receipt));

        assertThat(result).isEqualTo(response);
        verify(transactionMutationService).addReceiptsToTransaction(List.of(receipt), transaction);
    }

    @Test
    void shouldPropagateCategoryInactiveValidationDuringCreate() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        TransactionCreateRequestDTO dto = createRequest(
                BigDecimal.ONE,
                TransactionType.EXPENSE,
                4L,
                LocalDate.of(2026, 4, 1),
                "Expense",
                null
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
                4L,
                LocalDate.of(2026, 4, 1),
                "Expense",
                null
        );
        Transaction transaction = EntityTestFactory.transaction(null, user, null, null, dto.type(), dto.amount(), dto.date());

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(transactionValidationService.resolveAccount(user, dto.accountId())).thenReturn(defaultAccount);
        when(transactionValidationService.resolveActiveCategory(dto.categoryId(), user)).thenReturn(category);
        when(transactionMapper.toEntity(dto, user)).thenReturn(transaction);
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
                user,
                sourceAccount,
                expenseCategory,
                TransactionType.EXPENSE,
                new BigDecimal("30.00"),
                LocalDate.of(2026, 4, 1)
        );
        TransactionCreateRequestDTO updateDto = createRequest(
                new BigDecimal("50.00"),
                TransactionType.EXPENSE,
                4L,
                LocalDate.of(2026, 4, 2),
                "Updated expense",
                2L
        );
        TransactionResponseDTO response = new TransactionResponseDTO(
                9L,
                2L,
                null,
                updateDto.amount(),
                null,
                updateDto.description(),
                updateDto.type(),
                List.of(),
                updateDto.date(),
                null,
                null
        );

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(transactionRepository.findActiveByIdAndUser(9L, user)).thenReturn(Optional.of(existingTransaction));
        when(transactionValidationService.resolveAccount(user, 2L)).thenReturn(targetAccount);
        when(transactionValidationService.resolveActiveCategory(4L, user)).thenReturn(expenseCategory);
        when(transactionRepository.save(existingTransaction)).thenReturn(existingTransaction);
        when(transactionMutationService.toResponseDto(existingTransaction)).thenReturn(response);

        TransactionResponseDTO result = transactionService.updateTransaction(
                9L,
                authentication,
                updateDto,
                RecurringTransactionChangeScope.ONLY_THIS
        );

        assertThat(result).isEqualTo(response);
        verify(transactionMutationService).updateTransactionValues(existingTransaction, updateDto, targetAccount, expenseCategory);
        verify(transactionValidationService).validateOneTimeTransactionDate(updateDto.date(), user);
        verify(transactionValidationService).validateTransactionType(updateDto.type(), expenseCategory, user);
    }

    @Test
    void shouldDelegateRecurringCurrentAndFutureUpdateToRecurringService() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account account = EntityTestFactory.account(1L, user, BigDecimal.ZERO);
        Category salaryCategory = EntityTestFactory.category(4L, user, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        Transaction transaction = EntityTestFactory.transaction(
                9L,
                user,
                account,
                salaryCategory,
                TransactionType.INCOME,
                new BigDecimal("100.00"),
                LocalDate.of(2026, 4, 1)
        );
        TransactionCreateRequestDTO updateDto = createRequest(
                new BigDecimal("150.00"),
                TransactionType.INCOME,
                4L,
                LocalDate.of(2026, 4, 2),
                "Updated salary",
                1L
        );
        TransactionResponseDTO response = new TransactionResponseDTO(
                9L,
                1L,
                null,
                updateDto.amount(),
                null,
                updateDto.description(),
                updateDto.type(),
                List.of(),
                updateDto.date(),
                null,
                null
        );

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(transactionRepository.findActiveByIdAndUser(9L, user)).thenReturn(Optional.of(transaction));
        when(transactionValidationService.resolveAccount(user, 1L)).thenReturn(account);
        when(transactionValidationService.resolveActiveCategory(4L, user)).thenReturn(salaryCategory);
        when(transactionRepository.save(transaction)).thenReturn(transaction);
        when(transactionMutationService.toResponseDto(transaction)).thenReturn(response);

        TransactionResponseDTO result = transactionService.updateTransaction(
                9L,
                authentication,
                updateDto,
                RecurringTransactionChangeScope.THIS_AND_FUTURE
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
                user,
                account,
                category,
                TransactionType.INCOME,
                new BigDecimal("30.00"),
                LocalDate.of(2026, 4, 1)
        );
        UUID receiptId = UUID.randomUUID();
        EntityTestFactory.attachReceipts(transaction, new org.tc.mtracker.transaction.ReceiptImage(receiptId, transaction));

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(transactionRepository.findActiveByIdAndUser(9L, user)).thenReturn(Optional.of(transaction));

        transactionService.deleteTransaction(9L, authentication, RecurringTransactionChangeScope.ONLY_THIS);

        verify(transactionMutationService).deleteSingleTransaction(transaction);
    }

    @Test
    void shouldDelegateRecurringCurrentAndFutureDeleteToRecurringService() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account account = EntityTestFactory.account(1L, user, new BigDecimal("30.00"));
        Category category = EntityTestFactory.category(4L, user, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        Transaction transaction = EntityTestFactory.transaction(
                9L,
                user,
                account,
                category,
                TransactionType.INCOME,
                new BigDecimal("30.00"),
                LocalDate.of(2026, 4, 1)
        );

        when(userService.getCurrentAuthenticatedUser(authentication)).thenReturn(user);
        when(transactionRepository.findActiveByIdAndUser(9L, user)).thenReturn(Optional.of(transaction));

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
        when(transactionRepository.findActiveByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionById(99L, authentication))
                .isInstanceOf(TransactionNotFoundException.class);
    }
}
