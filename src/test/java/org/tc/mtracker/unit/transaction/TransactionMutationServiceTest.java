package org.tc.mtracker.unit.transaction;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.tc.mtracker.account.Account;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.category.enums.CategoryStatus;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.support.factory.EntityTestFactory;
import org.tc.mtracker.transaction.ReceiptImage;
import org.tc.mtracker.transaction.Transaction;
import org.tc.mtracker.transaction.TransactionMutationService;
import org.tc.mtracker.transaction.TransactionRepository;
import org.tc.mtracker.transaction.dto.TransactionCreateRequestDTO;
import org.tc.mtracker.transaction.dto.TransactionMapper;
import org.tc.mtracker.user.User;
import org.tc.mtracker.utils.S3Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class TransactionMutationServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private TransactionMutationService transactionMutationService;

    @Test
    void shouldPersistTransactionAndApplyIncomeBalanceDelta() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account account = EntityTestFactory.account(1L, user, new BigDecimal("10.00"));
        Category category = EntityTestFactory.category(4L, user, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        Transaction transaction = EntityTestFactory.transaction(
                null,
                user,
                account,
                category,
                TransactionType.INCOME,
                new BigDecimal("15.50"),
                LocalDate.of(2026, 4, 1)
        );

        when(transactionRepository.save(transaction)).thenReturn(transaction);

        Transaction result = transactionMutationService.persistTransaction(transaction);

        assertThat(result).isEqualTo(transaction);
        assertThat(account.getBalance()).isEqualByComparingTo("25.50");
    }

    @Test
    void shouldUpdateTransactionAndMoveBalanceBetweenAccounts() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account sourceAccount = EntityTestFactory.account(1L, user, new BigDecimal("70.00"));
        Account targetAccount = EntityTestFactory.account(2L, user, new BigDecimal("20.00"));
        Category category = EntityTestFactory.category(4L, user, "Groceries", TransactionType.EXPENSE, CategoryStatus.ACTIVE);
        Transaction transaction = EntityTestFactory.transaction(
                9L,
                user,
                sourceAccount,
                category,
                TransactionType.EXPENSE,
                new BigDecimal("30.00"),
                LocalDate.of(2026, 4, 1)
        );
        TransactionCreateRequestDTO updateDto = new TransactionCreateRequestDTO(
                new BigDecimal("50.00"),
                TransactionType.EXPENSE,
                4L,
                LocalDate.of(2026, 4, 2),
                "Updated expense",
                2L
        );
        doAnswer(invocation -> {
            TransactionCreateRequestDTO dto = invocation.getArgument(0);
            Transaction target = invocation.getArgument(1);
            target.setAmount(dto.amount());
            target.setType(dto.type());
            target.setDescription(dto.description());
            target.setDate(dto.date());
            return null;
        }).when(transactionMapper).updateEntity(eq(updateDto), eq(transaction));

        transactionMutationService.updateTransactionValues(transaction, updateDto, targetAccount, category);

        assertThat(sourceAccount.getBalance()).isEqualByComparingTo("100.00");
        assertThat(targetAccount.getBalance()).isEqualByComparingTo("-30.00");
        assertThat(transaction.getAccount()).isEqualTo(targetAccount);
        assertThat(transaction.getAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    void shouldDeleteTransactionRollbackBalanceAndDeleteReceiptObjects() {
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
        transaction.getReceipts().add(new ReceiptImage(receiptId, transaction));

        transactionMutationService.deleteSingleTransaction(transaction);

        assertThat(account.getBalance()).isEqualByComparingTo("0.00");
        verify(s3Service).deleteFile("receipts/" + receiptId);
        verify(transactionRepository).delete(transaction);
    }

    @Test
    void shouldAddReceiptObjectsWhenReceiptsAreProvided() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        Account account = EntityTestFactory.account(1L, user, BigDecimal.ZERO);
        Category category = EntityTestFactory.category(4L, user, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        Transaction transaction = EntityTestFactory.transaction(
                9L,
                user,
                account,
                category,
                TransactionType.INCOME,
                BigDecimal.ONE,
                LocalDate.of(2026, 4, 1)
        );
        MockMultipartFile receipt = new MockMultipartFile("receipts", "receipt.jpg", "image/jpeg", "receipt".getBytes());

        transactionMutationService.addReceiptsToTransaction(List.of(receipt), transaction);

        assertThat(transaction.getReceipts()).hasSize(1);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3Service).saveFile(keyCaptor.capture(), eq(receipt));
        assertThat(keyCaptor.getValue()).startsWith("receipts/");
    }
}
