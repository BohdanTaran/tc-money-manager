package org.tc.mtracker.integration.api;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.tc.mtracker.category.CategoryRepository;
import org.tc.mtracker.category.dto.CreateCategoryDTO;
import org.tc.mtracker.category.dto.UpdateCategoryDTO;
import org.tc.mtracker.category.enums.CategoryStatus;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.support.base.BaseApiIntegrationTest;
import org.tc.mtracker.user.User;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class CategoryApiTest extends BaseApiIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void shouldReturnGlobalAndOwnedCategories() {
        User currentUser = fixtures.createUser("categories@example.com");
        User otherUser = fixtures.createUser("other@example.com");
        fixtures.createGlobalCategory("Salary", TransactionType.INCOME);
        fixtures.createGlobalCategory("Rent", TransactionType.EXPENSE);
        fixtures.createUserCategory(currentUser, "Side Project", TransactionType.INCOME);
        fixtures.createCategory(currentUser, "Archived Food", TransactionType.EXPENSE, CategoryStatus.ARCHIVED, "archive");
        fixtures.createUserCategory(otherUser, "Hidden", TransactionType.EXPENSE);

        restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/categories")
                        .queryParam("type", TransactionType.INCOME, TransactionType.EXPENSE)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader(currentUser))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(3);
    }

    @Test
    void shouldFilterArchivedCategories() {
        User currentUser = fixtures.createUser("categories@example.com");
        fixtures.createUserCategory(currentUser, "Active", TransactionType.EXPENSE);
        fixtures.createCategory(currentUser, "Archived Food", TransactionType.EXPENSE, CategoryStatus.ARCHIVED, "archive");

        restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/categories")
                        .queryParam("type", TransactionType.EXPENSE)
                        .queryParam("archived", true)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader(currentUser))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("Archived Food");
    }

    @Test
    void shouldCreateCategory() {
        User user = fixtures.createUser("create-category@example.com");

        restTestClient.post()
                .uri("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .body(new CreateCategoryDTO("Health", TransactionType.EXPENSE, "heart"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Health")
                .jsonPath("$.type").isEqualTo("EXPENSE");

        assertThat(categoryRepository.findAll())
                .extracting("name")
                .contains("Health");
    }

    @Test
    void shouldRejectDuplicateCategoryNameAndTypeForSameVisibilityScope() {
        User user = fixtures.createUser("duplicate-category@example.com");
        fixtures.createGlobalCategory("Salary", TransactionType.INCOME);

        restTestClient.post()
                .uri("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .body(new CreateCategoryDTO("Salary", TransactionType.INCOME, "coin"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldUpdateOwnedCategory() {
        User user = fixtures.createUser("update-category@example.com");
        var category = fixtures.createUserCategory(user, "Freelance", TransactionType.INCOME);

        restTestClient.put()
                .uri("/api/v1/categories/{id}", category.getId())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .body(new UpdateCategoryDTO("Consulting", TransactionType.INCOME, "briefcase", CategoryStatus.ACTIVE))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Consulting");

        assertThat(categoryRepository.findById(category.getId()).orElseThrow().getName()).isEqualTo("Consulting");
    }

    @Test
    void shouldArchiveCategoryOnDelete() {
        User user = fixtures.createUser("delete-category@example.com");
        var category = fixtures.createUserCategory(user, "Travel", TransactionType.EXPENSE);

        restTestClient.delete()
                .uri("/api/v1/categories/{id}", category.getId())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isNoContent();

        assertThat(categoryRepository.findById(category.getId()).orElseThrow().getStatus()).isEqualTo(CategoryStatus.ARCHIVED);
    }
}
