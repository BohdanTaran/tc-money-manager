package org.tc.mtracker.integration.repository;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.tc.mtracker.account.Account;
import org.tc.mtracker.account.AccountRepository;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.category.CategoryRepository;
import org.tc.mtracker.category.enums.CategoryStatus;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.currency.CurrencyCode;
import org.tc.mtracker.support.base.BaseRepositoryIntegrationTest;
import org.tc.mtracker.transaction.Transaction;
import org.tc.mtracker.transaction.TransactionRepository;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class TransactionRepositoryTest extends BaseRepositoryIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void shouldFilterTransactionsAndKeepNewestFirst() {
        User currentUser = persistUser("user@example.com");
        User otherUser = persistUser("other@example.com");
        Account primaryAccount = currentUser.getDefaultAccount();
        Account savingsAccount = accountRepository.saveAndFlush(Account.builder()
                .user(currentUser)
                .balance(BigDecimal.ZERO)
                .build());
        Category incomeCategory = persistCategory(currentUser, "Salary", TransactionType.INCOME);
        Category expenseCategory = persistCategory(currentUser, "Groceries", TransactionType.EXPENSE);

        persistTransaction(currentUser, primaryAccount, incomeCategory, new BigDecimal("200.00"), TransactionType.INCOME, LocalDate.of(2026, 3, 10), null);
        Transaction filtered = persistTransaction(currentUser, savingsAccount, expenseCategory, new BigDecimal("40.00"), TransactionType.EXPENSE, LocalDate.of(2026, 4, 10), null);
        persistTransaction(otherUser, otherUser.getDefaultAccount(), incomeCategory, new BigDecimal("100.00"), TransactionType.INCOME, LocalDate.of(2026, 4, 11), null);
        persistTransaction(currentUser, primaryAccount, expenseCategory, new BigDecimal("15.00"), TransactionType.EXPENSE, LocalDate.of(2026, 4, 9), LocalDateTime.now());

        List<Transaction> allTransactions = transactionRepository.findAllByUserAndFilters(
                currentUser,
                null,
                null,
                null,
                null,
                null
        );

        List<Transaction> filteredTransactions = transactionRepository.findAllByUserAndFilters(
                currentUser,
                savingsAccount.getId(),
                expenseCategory.getId(),
                TransactionType.EXPENSE,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        );

        assertThat(allTransactions).extracting(Transaction::getAmount)
                .containsExactly(new BigDecimal("40.00"), new BigDecimal("200.00"));
        assertThat(filteredTransactions)
                .singleElement()
                .extracting(Transaction::getId)
                .isEqualTo(filtered.getId());
    }

    @Test
    void shouldReturnActiveTransactionsOnlyForOwningUser() {
        User currentUser = persistUser("user@example.com");
        User otherUser = persistUser("other@example.com");
        Category incomeCategory = persistCategory(currentUser, "Salary", TransactionType.INCOME);

        Transaction active = persistTransaction(currentUser, currentUser.getDefaultAccount(), incomeCategory, new BigDecimal("100.00"), TransactionType.INCOME, LocalDate.of(2026, 4, 1), null);
        Transaction deleted = persistTransaction(currentUser, currentUser.getDefaultAccount(), incomeCategory, new BigDecimal("10.00"), TransactionType.INCOME, LocalDate.of(2026, 4, 2), LocalDateTime.now());
        Transaction foreign = persistTransaction(otherUser, otherUser.getDefaultAccount(), incomeCategory, new BigDecimal("15.00"), TransactionType.INCOME, LocalDate.of(2026, 4, 3), null);

        assertThat(transactionRepository.findActiveByIdAndUser(active.getId(), currentUser)).isPresent();
        assertThat(transactionRepository.findActiveByIdAndUser(deleted.getId(), currentUser)).isEmpty();
        assertThat(transactionRepository.findActiveByIdAndUser(foreign.getId(), currentUser)).isEmpty();
    }

    private User persistUser(String email) {
        User user = userRepository.saveAndFlush(User.builder()
                .email(email)
                .password("encoded-password")
                .fullName("Test User")
                .currencyCode(CurrencyCode.USD)
                .activated(true)
                .build());

        Account account = accountRepository.saveAndFlush(Account.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .build());

        user.getAccounts().add(account);
        user.setDefaultAccount(account);
        return userRepository.saveAndFlush(user);
    }

    private Category persistCategory(User owner, String name, TransactionType type) {
        return categoryRepository.saveAndFlush(Category.builder()
                .user(owner)
                .name(name)
                .type(type)
                .status(CategoryStatus.ACTIVE)
                .icon("icon")
                .build());
    }

    private Transaction persistTransaction(
            User user,
            Account account,
            Category category,
            BigDecimal amount,
            TransactionType type,
            LocalDate date,
            LocalDateTime deletedAt
    ) {
        return transactionRepository.saveAndFlush(Transaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .amount(amount)
                .type(type)
                .date(date)
                .description("Test transaction")
                .deletedAt(deletedAt)
                .build());
    }
}
