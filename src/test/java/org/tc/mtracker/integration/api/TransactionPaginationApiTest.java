package org.tc.mtracker.integration.api;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.tc.mtracker.category.Category;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.support.base.BaseApiIntegrationTest;
import org.tc.mtracker.transaction.Transaction;
import org.tc.mtracker.user.User;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

@Tag("integration")
class TransactionPaginationApiTest extends BaseApiIntegrationTest {


    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnFirstPageWithoutCursor() {
        User user = fixtures.createUser("pagination@example.com");

        // Create 20 transactions
        for (int i = 1; i <= 20; i++) {
            fixtures.createTransaction(
                    user.getDefaultAccount(),
                    fixtures.createUserCategory(user, "Test " + i, TransactionType.EXPENSE),
                    new BigDecimal("10.00"),
                    TransactionType.EXPENSE,
                    LocalDate.now().minusDays(i),
                    "Transaction " + i
            );
        }

        restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("limit", 10)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(10)
                .jsonPath("$.hasNext").isEqualTo(true)
                .jsonPath("$.nextCursor").isNotEmpty()
                .jsonPath("$.pageSize").isEqualTo(10);
    }

    @Test
    void shouldReturnSecondPageWithCursor() {
        User user = fixtures.createUser("second-page@example.com");

        Category category = fixtures.createUserCategory(user, "Test", TransactionType.EXPENSE);

        // Make 15 transactions
        for (int i = 1; i <= 15; i++) {
            fixtures.createTransaction(
                    user.getDefaultAccount(),
                    category,
                    new BigDecimal("10.00"),
                    TransactionType.EXPENSE,
                    LocalDate.now().minusDays(i),
                    "Transaction " + i
            );
        }

        // Get first page
        String response = restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("limit", 10)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseBody();

        JsonNode jsonNode = objectMapper.readTree(response);
        String nextCursor = jsonNode.get("nextCursor").asString();

        // Second page with a cursor
        restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("limit", 10)
                        .queryParam("cursor", nextCursor)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(5)
                .jsonPath("$.hasNext").isEqualTo(false)
                .jsonPath("$.pageSize").isEqualTo(5);
    }

    @Test
    void shouldReturnLastPageWithLessThanLimit() {
        User user = fixtures.createUser("last-page@example.com");
        Category category = fixtures.createUserCategory(user, "Test", TransactionType.EXPENSE);

        // Make 8 transactions (8 < limit)
        for (int i = 1; i <= 8; i++) {
            fixtures.createTransaction(
                    user.getDefaultAccount(),
                    category,
                    new BigDecimal("10.00"),
                    TransactionType.EXPENSE,
                    LocalDate.now().minusDays(i),
                    "Transaction " + i
            );
        }

        restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("limit", 10)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(8)
                .jsonPath("$.hasNext").isEqualTo(false)
                .jsonPath("$.nextCursor").isEqualTo(null)
                .jsonPath("$.pageSize").isEqualTo(8);
    }

    @Test
    void shouldPaginateWithFilters() {
        User user = fixtures.createUser("filter-pagination@example.com");
        var groceries = fixtures.createUserCategory(user, "Groceries", TransactionType.EXPENSE);


        // // Make 25 transactions for Groceries
        for (int i = 1; i <= 25; i++) {
            fixtures.createTransaction(
                    user.getDefaultAccount(),
                    groceries,
                    new BigDecimal("10.00"),
                    TransactionType.EXPENSE,
                    LocalDate.now().minusDays(i),
                    "Groceries " + i
            );
        }

        // First page with filter
        restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("limit", 10)
                        .queryParam("categoryId", groceries.getId())
                        .queryParam("type", TransactionType.EXPENSE)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(10)
                .jsonPath("$.hasNext").isEqualTo(true)
                .jsonPath("$.nextCursor").isNotEmpty();
    }

    @Test
    void shouldPaginateWithDescriptionSearch() {
        User user = fixtures.createUser("description-pagination@example.com");
        Category category = fixtures.createUserCategory(user, "Coffee", TransactionType.EXPENSE);

        for (int i = 1; i <= 12; i++) {
            fixtures.createTransaction(
                    user.getDefaultAccount(),
                    category,
                    new BigDecimal("10.00"),
                    TransactionType.EXPENSE,
                    LocalDate.now().minusDays(i),
                    "Coffee stop " + i
            );
        }
        for (int i = 1; i <= 5; i++) {
            fixtures.createTransaction(
                    user.getDefaultAccount(),
                    category,
                    new BigDecimal("10.00"),
                    TransactionType.EXPENSE,
                    LocalDate.now().minusDays(20 + i),
                    "Lunch " + i
            );
        }

        String response = restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("limit", 10)
                        .queryParam("description", "coffee")
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode jsonNode = objectMapper.readTree(response);
        String nextCursor = jsonNode.get("nextCursor").asString();

        restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("limit", 10)
                        .queryParam("description", "coffee")
                        .queryParam("cursor", nextCursor)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(2)
                .jsonPath("$.hasNext").isEqualTo(false)
                .jsonPath("$.nextCursor").isEqualTo(null)
                .jsonPath("$.pageSize").isEqualTo(2);
    }

    @Test
    void shouldReturnEmptyPageWhenNoTransactions() {
        User user = fixtures.createUser("empty@example.com");

        restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("limit", 10)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(0)
                .jsonPath("$.hasNext").isEqualTo(false)
                .jsonPath("$.nextCursor").isEqualTo(null)
                .jsonPath("$.pageSize").isEqualTo(0);
    }

    @Test
    void shouldHandleInvalidCursorGracefully() {
        User user = fixtures.createUser("invalid-cursor@example.com");

        fixtures.createTransaction(
                user.getDefaultAccount(),
                fixtures.createUserCategory(user, "Test", TransactionType.EXPENSE),
                new BigDecimal("10.00"),
                TransactionType.EXPENSE,
                LocalDate.now(),
                "Transaction"
        );

        restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("limit", 10)
                        .queryParam("cursor", "invalid-cursor-string")
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.hasNext").isEqualTo(false);
    }

    @Test
    void shouldRespectMinMaxLimit() {
        User user = fixtures.createUser("limit-boundaries@example.com");
        var category = fixtures.createUserCategory(user, "Test", TransactionType.EXPENSE);

        // Make 30 transactions
        for (int i = 1; i <= 30; i++) {
            fixtures.createTransaction(
                    user.getDefaultAccount(),
                    category,
                    new BigDecimal("10.00"),
                    TransactionType.EXPENSE,
                    LocalDate.now().minusDays(i % 30),
                    "Transaction " + i
            );
        }

        // Limit = 1 (minimum)
        restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("limit", 1)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.hasNext").isEqualTo(true);

        // Limit = 20 (reasonable)
        restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("limit", 20)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(20)
                .jsonPath("$.hasNext").isEqualTo(true);

        // Limit = 101 (above maximum - should return error)
        restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("limit", 101)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void shouldReturnTransactionsSortedByDateDescending() {
        User user = fixtures.createUser("sorting@example.com");

        Category category = fixtures.createUserCategory(user, "Test", TransactionType.EXPENSE);

        LocalDate today = LocalDate.now();
        Transaction t1 = fixtures.createTransaction(
                user.getDefaultAccount(),
                category,
                new BigDecimal("10.00"),
                TransactionType.EXPENSE,
                today.minusDays(1),
                "Yesterday"
        );
        Transaction t2 = fixtures.createTransaction(
                user.getDefaultAccount(),
                category,
                new BigDecimal("20.00"),
                TransactionType.EXPENSE,
                today,
                "Today"
        );
        Transaction t3 = fixtures.createTransaction(
                user.getDefaultAccount(),
                category,
                new BigDecimal("30.00"),
                TransactionType.EXPENSE,
                today.minusDays(2),
                "Two days ago"
        );

        restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("limit", 10)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].date").isEqualTo(today.toString())
                .jsonPath("$.data[0].id").isEqualTo(t2.getId())
                .jsonPath("$.data[1].date").isEqualTo(today.minusDays(1).toString())
                .jsonPath("$.data[1].id").isEqualTo(t1.getId())
                .jsonPath("$.data[2].date").isEqualTo(today.minusDays(2).toString())
                .jsonPath("$.data[2].id").isEqualTo(t3.getId());
    }

    @Test
    void shouldHandleLargePagination() {
        User user = fixtures.createUser("large-pagination@example.com");

        Category category = fixtures.createUserCategory(user, "Test", TransactionType.EXPENSE);

        // Make 200 transactions
        for (int i = 1; i <= 200; i++) {
            fixtures.createTransaction(
                    user.getDefaultAccount(),
                    category,
                    new BigDecimal("10.00"),
                    TransactionType.EXPENSE,
                    LocalDate.now().minusDays(i % 365),
                    "Transaction " + i
            );
        }

        // First page
        String firstPageResponse = restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("limit", 50)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseBody();

        JsonNode jsonNode = objectMapper.readTree(firstPageResponse);
        String cursor = jsonNode.get("nextCursor").asString();

        // Second page
        restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("limit", 50)
                        .queryParam("cursor", cursor)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(50)
                .jsonPath("$.hasNext").isEqualTo(true);
    }

    @Test
    void shouldReturnBadRequestWhenDescriptionIsTooShort() {
        User user = fixtures.createUser("short-search@example.com");

        restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("limit", 10)
                        .queryParam("description", "ab")
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturnBadRequestWhenDescriptionContainsOnlySpaces() {
        User user = fixtures.createUser("spaces-search@example.com");

        restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("limit", 10)
                        .queryParam("description", "   ")
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isBadRequest();
    }
}
