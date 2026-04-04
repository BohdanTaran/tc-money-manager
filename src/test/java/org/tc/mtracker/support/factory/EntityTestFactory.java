package org.tc.mtracker.support.factory;

import org.tc.mtracker.account.Account;
import org.tc.mtracker.auth.model.RefreshToken;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.category.enums.CategoryStatus;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.currency.CurrencyCode;
import org.tc.mtracker.transaction.ReceiptImage;
import org.tc.mtracker.transaction.Transaction;
import org.tc.mtracker.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class EntityTestFactory {

    private EntityTestFactory() {
    }

    public static User user(Long id, String email, boolean activated) {
        return User.builder()
                .id(id)
                .email(email)
                .password("encoded-password")
                .fullName("Test User")
                .currencyCode(CurrencyCode.USD)
                .activated(activated)
                .build();
    }

    public static Account account(Long id, User user, BigDecimal balance) {
        return Account.builder()
                .id(id)
                .user(user)
                .balance(balance)
                .build();
    }

    public static Category category(Long id, User user, String name, TransactionType type, CategoryStatus status) {
        return Category.builder()
                .id(id)
                .user(user)
                .name(name)
                .type(type)
                .status(status)
                .icon("icon")
                .build();
    }

    public static Transaction transaction(
            Long id,
            User user,
            Account account,
            Category category,
            TransactionType type,
            BigDecimal amount,
            LocalDate date
    ) {
        return Transaction.builder()
                .id(id)
                .user(user)
                .account(account)
                .category(category)
                .type(type)
                .amount(amount)
                .description("Test transaction")
                .date(date)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static ReceiptImage receipt(Transaction transaction) {
        return new ReceiptImage(UUID.randomUUID(), transaction);
    }

    public static RefreshToken refreshToken(String token, User user, LocalDateTime expiryDate) {
        return RefreshToken.builder()
                .token(token)
                .user(user)
                .expiryDate(expiryDate)
                .build();
    }

    public static void linkDefaultAccount(User user, Account account) {
        user.getAccounts().add(account);
        user.setDefaultAccount(account);
    }

    public static void attachReceipts(Transaction transaction, ReceiptImage... receipts) {
        transaction.getReceipts().clear();
        transaction.getReceipts().addAll(List.of(receipts));
    }
}
