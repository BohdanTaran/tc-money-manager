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
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class CategoryRepositoryTest extends BaseRepositoryIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void shouldReturnOnlyGlobalAndOwnedCategoriesMatchingFilters() {
        User currentUser = persistUser("user@example.com");
        User otherUser = persistUser("other@example.com");

        persistCategory(null, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        persistCategory(null, "Rent", TransactionType.EXPENSE, CategoryStatus.ACTIVE);
        persistCategory(currentUser, "Side Project", TransactionType.INCOME, CategoryStatus.ACTIVE);
        persistCategory(currentUser, "Archived Food", TransactionType.EXPENSE, CategoryStatus.ARCHIVED);
        persistCategory(otherUser, "Secret Hobby", TransactionType.EXPENSE, CategoryStatus.ACTIVE);

        List<Category> result = categoryRepository.findGlobalAndUserCategories(
                currentUser,
                null,
                List.of(TransactionType.INCOME, TransactionType.EXPENSE),
                CategoryStatus.ACTIVE
        );

        assertThat(result)
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("Salary", "Rent", "Side Project");
    }

    @Test
    void shouldResolveAccessibleCategoriesByOwnershipOrGlobalVisibility() {
        User currentUser = persistUser("user@example.com");
        User otherUser = persistUser("other@example.com");

        Category globalCategory = persistCategory(null, "Salary", TransactionType.INCOME, CategoryStatus.ACTIVE);
        Category ownedCategory = persistCategory(currentUser, "Freelance", TransactionType.INCOME, CategoryStatus.ACTIVE);
        Category foreignCategory = persistCategory(otherUser, "Hidden", TransactionType.EXPENSE, CategoryStatus.ACTIVE);

        assertThat(categoryRepository.findAccessibleById(globalCategory.getId(), currentUser)).isPresent();
        assertThat(categoryRepository.findAccessibleById(ownedCategory.getId(), currentUser)).isPresent();
        assertThat(categoryRepository.findAccessibleById(foreignCategory.getId(), currentUser)).isEmpty();
        assertThat(categoryRepository.findOwnedById(ownedCategory.getId(), currentUser)).isPresent();
        assertThat(categoryRepository.findOwnedById(globalCategory.getId(), currentUser)).isEmpty();
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

    private Category persistCategory(User user, String name, TransactionType type, CategoryStatus status) {
        return categoryRepository.saveAndFlush(Category.builder()
                .user(user)
                .name(name)
                .type(type)
                .status(status)
                .icon("icon")
                .build());
    }
}
