package org.tc.mtracker.support.factory;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.tc.mtracker.account.Account;
import org.tc.mtracker.account.AccountRepository;
import org.tc.mtracker.auth.model.RefreshToken;
import org.tc.mtracker.auth.repository.RefreshTokenRepository;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.category.CategoryRepository;
import org.tc.mtracker.category.enums.CategoryStatus;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.currency.CurrencyCode;
import org.tc.mtracker.transaction.Transaction;
import org.tc.mtracker.transaction.TransactionRepository;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class DatabaseTestDataFactory {

    public static final String DEFAULT_PASSWORD = "StrongPass!1";

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    private final AtomicInteger sequence = new AtomicInteger();

    public User createUser() {
        int suffix = sequence.incrementAndGet();
        return createUser(
                "user-%d@example.com".formatted(suffix),
                "Test User %d".formatted(suffix),
                true,
                CurrencyCode.USD,
                BigDecimal.ZERO,
                DEFAULT_PASSWORD
        );
    }

    public User createUser(String email) {
        return createUser(email, "Test User", true, CurrencyCode.USD, BigDecimal.ZERO, DEFAULT_PASSWORD);
    }

    public User createUser(String email, boolean activated) {
        return createUser(email, "Test User", activated, CurrencyCode.USD, BigDecimal.ZERO, DEFAULT_PASSWORD);
    }

    public User createUser(String email, boolean activated, BigDecimal defaultBalance) {
        return createUser(email, "Test User", activated, CurrencyCode.USD, defaultBalance, DEFAULT_PASSWORD);
    }

    public User createUser(
            String email,
            String fullName,
            boolean activated,
            CurrencyCode currencyCode,
            BigDecimal defaultBalance,
            String rawPassword
    ) {
        User user = userRepository.saveAndFlush(User.builder()
                .email(email)
                .fullName(fullName)
                .password(passwordEncoder.encode(rawPassword))
                .activated(activated)
                .currencyCode(currencyCode)
                .build());

        Account defaultAccount = accountRepository.saveAndFlush(Account.builder()
                .user(user)
                .balance(defaultBalance)
                .build());

        user.getAccounts().add(defaultAccount);
        user.setDefaultAccount(defaultAccount);
        return userRepository.saveAndFlush(user);
    }

    public Account createAccount(User user, BigDecimal balance) {
        return accountRepository.saveAndFlush(Account.builder()
                .user(user)
                .balance(balance)
                .build());
    }

    public Category createGlobalCategory(String name, TransactionType type) {
        return createCategory(null, name, type, CategoryStatus.ACTIVE, "icon");
    }

    public Category createUserCategory(User user, String name, TransactionType type) {
        return createCategory(user, name, type, CategoryStatus.ACTIVE, "icon");
    }

    public Category createCategory(User user, String name, TransactionType type, CategoryStatus status, String icon) {
        return categoryRepository.saveAndFlush(Category.builder()
                .user(user)
                .name(name)
                .type(type)
                .status(status)
                .icon(icon)
                .build());
    }

    public Transaction createTransaction(
            User user,
            Account account,
            Category category,
            BigDecimal amount,
            TransactionType type,
            LocalDate date,
            String description
    ) {
        Transaction transaction = transactionRepository.saveAndFlush(Transaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .amount(amount)
                .type(type)
                .date(date)
                .description(description)
                .build());

        BigDecimal delta = type == TransactionType.INCOME ? amount : amount.negate();
        account.setBalance(account.getBalance().add(delta));
        accountRepository.saveAndFlush(account);

        return transaction;
    }

    public RefreshToken createRefreshToken(User user, String token) {
        return createRefreshToken(user, token, LocalDateTime.now().plusDays(1));
    }

    public RefreshToken createRefreshToken(User user, String token, LocalDateTime expiryDate) {
        refreshTokenRepository.deleteByUser(user);
        return refreshTokenRepository.saveAndFlush(RefreshToken.builder()
                .user(user)
                .token(token)
                .expiryDate(expiryDate)
                .build());
    }
}
