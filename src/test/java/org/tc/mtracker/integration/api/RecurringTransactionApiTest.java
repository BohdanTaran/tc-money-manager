package org.tc.mtracker.integration.api;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.tc.mtracker.account.AccountRepository;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.support.base.BaseApiIntegrationTest;
import org.tc.mtracker.support.factory.MultipartTestResourceFactory;
import org.tc.mtracker.transaction.TransactionRepository;
import org.tc.mtracker.transaction.dto.TransactionCreateRequestDTO;
import org.tc.mtracker.transaction.recurring.RecurringTransactionRepository;
import org.tc.mtracker.transaction.recurring.enums.IntervalUnit;
import org.tc.mtracker.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class RecurringTransactionApiTest extends BaseApiIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    private static TransactionCreateRequestDTO createRequest(
            BigDecimal amount,
            TransactionType type,
            Long categoryId,
            LocalDate date,
            String description,
            Long accountId,
            IntervalUnit intervalUnit
    ) {
        return new TransactionCreateRequestDTO(
                amount,
                type,
                categoryId,
                date,
                description,
                accountId,
                intervalUnit
        );
    }

    private static MultipartBodyBuilder createMultipartRequest(TransactionCreateRequestDTO request) {
        MultipartBodyBuilder parts = new MultipartBodyBuilder();
        parts.part("dto", request, MediaType.APPLICATION_JSON);
        return parts;
    }

    private static MultipartBodyBuilder createMultipartRequest(
            TransactionCreateRequestDTO request,
            ByteArrayResource receipt,
            MediaType receiptMediaType
    ) {
        MultipartBodyBuilder parts = createMultipartRequest(request);
        parts.part("receipts", receipt, receiptMediaType);
        return parts;
    }

    @Test
    void shouldCreateRecurringTransactionForTodayAndExecuteImmediately() {
        User user = fixtures.createUser("recurring-today@example.com");
        var category = fixtures.createGlobalCategory("Salary", TransactionType.INCOME);

        restTestClient.post()
                .uri("/api/v1/transactions")
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .body(createMultipartRequest(createRequest(
                        new BigDecimal("100.00"),
                        TransactionType.INCOME,
                        category.getId(),
                        LocalDate.now(),
                        "Recurring salary",
                        null,
                        IntervalUnit.MONTHLY
                )).build())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.date").isEqualTo(LocalDate.now().toString())
                .jsonPath("$.intervalUnit").isEqualTo("MONTHLY");

        assertThat(transactionRepository.findAll()).hasSize(1);
        assertThat(recurringTransactionRepository.findAll()).singleElement()
                .satisfies(recurringTransaction -> {
                    assertThat(recurringTransaction.getStartDate()).isEqualTo(LocalDate.now());
                    assertThat(recurringTransaction.getNextExecutionDate()).isEqualTo(LocalDate.now().plusMonths(1));
                });
        assertThat(accountRepository.findById(user.getDefaultAccount().getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void shouldDeleteRecurringRuleAndKeepGeneratedTransactions() {
        User user = fixtures.createUser("delete-recurring-rule@example.com");
        var category = fixtures.createGlobalCategory("Salary", TransactionType.INCOME);

        restTestClient.post()
                .uri("/api/v1/transactions")
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .body(createMultipartRequest(createRequest(
                        new BigDecimal("100.00"),
                        TransactionType.INCOME,
                        category.getId(),
                        LocalDate.now(),
                        "Recurring salary",
                        null,
                        IntervalUnit.MONTHLY
                )).build())
                .exchange()
                .expectStatus().isCreated();

        var recurringTransaction = recurringTransactionRepository.findAll().getFirst();

        restTestClient.delete()
                .uri("/api/v1/recurring-transactions/{id}", recurringTransaction.getId())
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isNoContent();

        assertThat(recurringTransactionRepository.findAll()).isEmpty();
        assertThat(transactionRepository.findAll()).singleElement()
                .satisfies(transaction -> assertThat(transaction.getRecurringTransaction()).isNull());
        assertThat(accountRepository.findById(user.getDefaultAccount().getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void shouldCreateRecurringTransactionForFutureWithoutImmediateExecution() {
        User user = fixtures.createUser("recurring-future@example.com");
        var category = fixtures.createGlobalCategory("Salary", TransactionType.INCOME);
        LocalDate futureDate = LocalDate.now().plusDays(10);

        restTestClient.post()
                .uri("/api/v1/transactions")
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .body(createMultipartRequest(createRequest(
                        new BigDecimal("100.00"),
                        TransactionType.INCOME,
                        category.getId(),
                        futureDate,
                        "Future recurring salary",
                        null,
                        IntervalUnit.MONTHLY
                )).build())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.date").isEqualTo(futureDate.toString())
                .jsonPath("$.intervalUnit").isEqualTo("MONTHLY");

        assertThat(transactionRepository.findAll()).isEmpty();
        assertThat(recurringTransactionRepository.findAll()).singleElement()
                .satisfies(recurringTransaction -> {
                    assertThat(recurringTransaction.getStartDate()).isEqualTo(futureDate);
                    assertThat(recurringTransaction.getNextExecutionDate()).isEqualTo(futureDate);
                });
        assertThat(accountRepository.findById(user.getDefaultAccount().getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("0.00");
    }

    @Test
    void shouldRejectReceiptUploadForFutureRecurringTransaction() {
        User user = fixtures.createUser("recurring-future-receipt@example.com");
        var category = fixtures.createGlobalCategory("Salary", TransactionType.INCOME);
        LocalDate futureDate = LocalDate.now().plusDays(10);

        restTestClient.post()
                .uri("/api/v1/transactions")
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .body(createMultipartRequest(
                        createRequest(
                                new BigDecimal("100.00"),
                                TransactionType.INCOME,
                                category.getId(),
                                futureDate,
                                "Future recurring salary",
                                null,
                                IntervalUnit.MONTHLY
                        ),
                        MultipartTestResourceFactory.jpegImage("receipt.jpg"),
                        MediaType.IMAGE_JPEG
                ).build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("invalid_receipt_attachment");

        assertThat(transactionRepository.findAll()).isEmpty();
        assertThat(recurringTransactionRepository.findAll()).isEmpty();
    }

    @Test
    void shouldRejectRecurringTransactionWithPastStartDate() {
        User user = fixtures.createUser("recurring-past@example.com");
        var category = fixtures.createGlobalCategory("Salary", TransactionType.INCOME);

        restTestClient.post()
                .uri("/api/v1/transactions")
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .body(createMultipartRequest(createRequest(
                        new BigDecimal("100.00"),
                        TransactionType.INCOME,
                        category.getId(),
                        LocalDate.now().minusDays(1),
                        "Past recurring salary",
                        null,
                        IntervalUnit.MONTHLY
                )).build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("invalid_transaction_date");
    }
}
