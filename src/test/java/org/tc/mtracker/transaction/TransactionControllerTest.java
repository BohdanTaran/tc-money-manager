package org.tc.mtracker.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.account.AccountRepository;
import org.tc.mtracker.common.enums.TransactionType;
import org.tc.mtracker.transaction.dto.TransactionCreateRequestDTO;
import org.tc.mtracker.transaction.dto.TransactionResponseDTO;
import org.tc.mtracker.utils.S3Service;
import org.tc.mtracker.utils.TestHelpers;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@AutoConfigureRestTestClient
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "security.jwt.secret-key=aZbYcXdWeVfUgThSiRjQkPlOmNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRrQqPpOoNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRrQqPpOoNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRrQqPpOoNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRrQqPpOoNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRr",
                "security.jwt.expiration-time=900",
                "security.jwt.test-expiration=60",
                "aws.region=us-central-1",
                "aws.s3.bucket-name=test-bucket",
                "aws.access-key-id=test-key-id",
                "aws.secret-access-key=test-secret"
        }
)
@Testcontainers
@Sql(value = "/datasets/test_categories.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = "/datasets/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class TransactionControllerTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.3.0");

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private TestHelpers testHelpers;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @MockitoBean
    private S3Service s3Service;

    private String authToken;

    @BeforeEach
    void setUp() {
        authToken = "Bearer " + testHelpers.generateTestToken("test@gmail.com", "access_token", 3600000);
        Mockito.when(s3Service.generatePresignedUrl(anyString())).thenReturn("https://example.com/test-receipt.jpg");
    }

    @Test
    void shouldReturn201WhenTransactionIsCreatedWithoutReceipts() {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("dto", buildValidDto(), MediaType.APPLICATION_JSON);

        restTestClient.post()
                .uri("/api/v1/transactions")
                .body(multipartBodyBuilder.build())
                .header(HttpHeaders.AUTHORIZATION, authToken)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.receiptsUrls.length()").isEqualTo(0)
                .jsonPath("$.accountId").isEqualTo(1);

        assertThat(accountRepository.findById(1L).orElseThrow().getBalance())
                .isEqualByComparingTo("1.00");

        verifyNoInteractions(s3Service);
    }

    @Test
    @SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
    @Sql(statements = "INSERT INTO accounts (id, user_id, balance) VALUES (10, 1, 0.00)")
    void shouldReturn201WhenTransactionIsCreatedForProvidedAccount() {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("dto", buildValidDto(10L), MediaType.APPLICATION_JSON);

        restTestClient.post()
                .uri("/api/v1/transactions")
                .body(multipartBodyBuilder.build())
                .header(HttpHeaders.AUTHORIZATION, authToken)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.accountId").isEqualTo(10);

        assertThat(accountRepository.findById(1L).orElseThrow().getBalance())
                .isEqualByComparingTo("0.00");
        assertThat(accountRepository.findById(10L).orElseThrow().getBalance())
                .isEqualByComparingTo("1.00");

        verifyNoInteractions(s3Service);
    }

    @Test
    void shouldReturn201WhenTransactionIsCreatedWithReceiptInJPEG() {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("dto", buildValidDto(), MediaType.APPLICATION_JSON);
        multipartBodyBuilder.part("receipts", buildResource("test-receipt1.jpg", "jpg-data"), MediaType.IMAGE_JPEG);

        restTestClient.post()
                .uri("/api/v1/transactions")
                .body(multipartBodyBuilder.build())
                .header(HttpHeaders.AUTHORIZATION, authToken)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.receiptsUrls.length()").isEqualTo(1);

        verify(s3Service, times(1)).saveFile(anyString(), any(MultipartFile.class));
        verify(s3Service, times(1)).generatePresignedUrl(anyString());
    }

    @Test
    void shouldReturn201WhenTransactionIsCreatedWithReceiptInPDF() {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("dto", buildValidDto(), MediaType.APPLICATION_JSON);
        multipartBodyBuilder.part(
                "receipts",
                buildResource("test-receipt1.pdf", "%PDF-1.7 test document"),
                MediaType.APPLICATION_PDF
        );

        restTestClient.post()
                .uri("/api/v1/transactions")
                .body(multipartBodyBuilder.build())
                .header(HttpHeaders.AUTHORIZATION, authToken)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.receiptsUrls.length()").isEqualTo(1);

        verify(s3Service, times(1)).saveFile(anyString(), any(MultipartFile.class));
        verify(s3Service, times(1)).generatePresignedUrl(anyString());
    }

    @Test
    void shouldReturn400WhenReceiptFormatIsInvalid() {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("dto", buildValidDto(), MediaType.APPLICATION_JSON);
        multipartBodyBuilder.part(
                "receipts",
                buildResource("test-receipt1.txt", "plain-text"),
                MediaType.TEXT_PLAIN
        );

        restTestClient.post()
                .uri("/api/v1/transactions")
                .body(multipartBodyBuilder.build())
                .header(HttpHeaders.AUTHORIZATION, authToken)
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(s3Service);
    }

    @Test
    void shouldReturn400WhenReceiptExtensionIsInvalidEvenWithValidMimeType() {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("dto", buildValidDto(), MediaType.APPLICATION_JSON);
        multipartBodyBuilder.part(
                "receipts",
                buildResource("test-receipt1.txt", "fake-jpeg-data"),
                MediaType.IMAGE_JPEG
        );

        restTestClient.post()
                .uri("/api/v1/transactions")
                .body(multipartBodyBuilder.build())
                .header(HttpHeaders.AUTHORIZATION, authToken)
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(s3Service);
    }

    @Test
    @SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
    @Sql(statements = "INSERT INTO categories (id, name, type, status, user_id, created_at, updated_at) " +
            "VALUES (6, 'Archived Hobby', 'INCOME', 'ARCHIVED', 1, NOW(), NOW())")
    void shouldReturn400WhenCategoryIsArchived() {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("dto", buildDtoWithInvalidCategory(), MediaType.APPLICATION_JSON);

        restTestClient.post()
                .uri("/api/v1/transactions")
                .body(multipartBodyBuilder.build())
                .header(HttpHeaders.AUTHORIZATION, authToken)
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(s3Service);
    }

    @Test
    @SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
    @Sql(statements = {
            "INSERT INTO transactions (id, user_id, account_id, category_id, amount, type, date, description, created_at, updated_at) " +
                    "VALUES (90, 1, 1, 1, 100.00, 'INCOME', '2026-04-01', 'Salary', NOW(), NOW())",
            "INSERT INTO transactions (id, user_id, account_id, category_id, amount, type, date, description, created_at, updated_at) " +
                    "VALUES (91, 1, 1, 2, 20.00, 'EXPENSE', '2026-04-02', 'Groceries', NOW(), NOW())",
            "INSERT INTO transactions (id, user_id, account_id, category_id, amount, type, date, description, created_at, updated_at) " +
                    "VALUES (92, 2, 2, 1, 75.00, 'INCOME', '2026-04-03', 'Other user income', NOW(), NOW())"
    })
    void shouldReturnTransactionsWithoutFilters() {
        restTestClient.get()
                .uri("/api/v1/transactions")
                .header(HttpHeaders.AUTHORIZATION, authToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].id").isEqualTo(91)
                .jsonPath("$[1].id").isEqualTo(90);

        verifyNoInteractions(s3Service);
    }

    @Test
    @SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
    @Sql(statements = {
            "INSERT INTO accounts (id, user_id, balance) VALUES (10, 1, 0.00)",
            "INSERT INTO transactions (id, user_id, account_id, category_id, amount, type, date, description, created_at, updated_at) " +
                    "VALUES (93, 1, 1, 1, 200.00, 'INCOME', '2026-03-10', 'Salary', NOW(), NOW())",
            "INSERT INTO transactions (id, user_id, account_id, category_id, amount, type, date, description, created_at, updated_at) " +
                    "VALUES (94, 1, 10, 2, 40.00, 'EXPENSE', '2026-04-10', 'April groceries', NOW(), NOW())",
            "INSERT INTO transactions (id, user_id, account_id, category_id, amount, type, date, description, created_at, updated_at) " +
                    "VALUES (95, 1, 10, 2, 55.00, 'EXPENSE', '2026-05-10', 'May groceries', NOW(), NOW())"
    })
    void shouldReturnTransactionsWithFilters() {
        restTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("accountId", 10)
                        .queryParam("categoryId", 2)
                        .queryParam("type", TransactionType.EXPENSE)
                        .queryParam("dateFrom", "2026-04-01")
                        .queryParam("dateTo", "2026-04-30")
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].id").isEqualTo(94)
                .jsonPath("$[0].accountId").isEqualTo(10)
                .jsonPath("$[0].type").isEqualTo("EXPENSE")
                .jsonPath("$[0].category.id").isEqualTo(2);

        verifyNoInteractions(s3Service);
    }

    @Test
    @SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
    @Sql(statements = "INSERT INTO transactions (id, user_id, account_id, category_id, amount, type, date, description, created_at, updated_at) " +
            "VALUES (96, 1, 1, 1, 333.00, 'INCOME', '2026-04-05', 'Bonus', NOW(), NOW())")
    void shouldReturnTransactionById() {
        restTestClient.get()
                .uri("/api/v1/transactions/96")
                .header(HttpHeaders.AUTHORIZATION, authToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TransactionResponseDTO.class)
                .value(response -> {
                    assertThat(response.id()).isEqualTo(96L);
                    assertThat(response.accountId()).isEqualTo(1L);
                    assertThat(response.type()).isEqualTo(TransactionType.INCOME);
                    assertThat(response.amount()).isEqualByComparingTo("333.00");
                    assertThat(response.description()).isEqualTo("Bonus");
                });

        verifyNoInteractions(s3Service);
    }

    @Test
    @SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
    @Sql(statements = "INSERT INTO transactions (id, user_id, account_id, category_id, amount, type, date, description, created_at, updated_at) " +
            "VALUES (97, 2, 2, 1, 15.00, 'INCOME', '2026-04-05', 'Other user bonus', NOW(), NOW())")
    void shouldReturn404WhenTransactionBelongsToAnotherUser() {
        restTestClient.get()
                .uri("/api/v1/transactions/97")
                .header(HttpHeaders.AUTHORIZATION, authToken)
                .exchange()
                .expectStatus().isNotFound();

        verifyNoInteractions(s3Service);
    }

    @Test
    @SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
    @Sql(statements = {
            "INSERT INTO accounts (id, user_id, balance) VALUES (10, 1, 0.00)",
            "INSERT INTO transactions (id, user_id, account_id, category_id, amount, type, date, description, created_at, updated_at) " +
                    "VALUES (100, 1, 1, 1, 10.00, 'INCOME', CURRENT_DATE, 'Initial salary', NOW(), NOW())",
            "UPDATE accounts SET balance = 10.00 WHERE id = 1"
    })
    void shouldUpdateTransactionAndRecalculateBalances() {
        TransactionCreateRequestDTO updateDto = new TransactionCreateRequestDTO(
                BigDecimal.valueOf(6.00),
                TransactionType.EXPENSE,
                2L,
                LocalDate.now(),
                "Updated rent",
                10L
        );

        restTestClient.put()
                .uri("/api/v1/transactions/100")
                .body(updateDto)
                .header(HttpHeaders.AUTHORIZATION, authToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TransactionResponseDTO.class)
                .value(response -> {
                    assertThat(response.id()).isEqualTo(100L);
                    assertThat(response.accountId()).isEqualTo(10L);
                    assertThat(response.type()).isEqualTo(TransactionType.EXPENSE);
                    assertThat(response.description()).isEqualTo("Updated rent");
                    assertThat(response.amount()).isEqualByComparingTo("6.0");
                });

        assertThat(accountRepository.findById(1L).orElseThrow().getBalance())
                .isEqualByComparingTo("0.00");
        assertThat(accountRepository.findById(10L).orElseThrow().getBalance())
                .isEqualByComparingTo("-6.00");

        verifyNoInteractions(s3Service);
    }

    @Test
    @SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
    @Sql(statements = {
            "INSERT INTO transactions (id, user_id, account_id, category_id, amount, type, date, description, created_at, updated_at) " +
                    "VALUES (101, 1, 1, 2, 7.00, 'EXPENSE', CURRENT_DATE, 'Groceries', NOW(), NOW())",
            "UPDATE accounts SET balance = -7.00 WHERE id = 1"
    })
    void shouldDeleteTransactionAndRollbackBalance() {
        restTestClient.delete()
                .uri("/api/v1/transactions/101")
                .header(HttpHeaders.AUTHORIZATION, authToken)
                .exchange()
                .expectStatus().isNoContent();

        assertThat(accountRepository.findById(1L).orElseThrow().getBalance())
                .isEqualByComparingTo("0.00");
        assertThat(transactionRepository.findById(101L)).isEmpty();

        verifyNoInteractions(s3Service);
    }

    private static TransactionCreateRequestDTO buildValidDto() {
        return buildValidDto(null);
    }

    private static TransactionCreateRequestDTO buildValidDto(Long accountId) {
        return new TransactionCreateRequestDTO(
                BigDecimal.valueOf(1.0),
                TransactionType.INCOME,
                1L,
                LocalDate.now(),
                "Shop",
                accountId
        );
    }

    private static TransactionCreateRequestDTO buildDtoWithInvalidCategory() {
        return new TransactionCreateRequestDTO(
                BigDecimal.valueOf(1.0),
                TransactionType.INCOME,
                6L,
                LocalDate.now(),
                "Shop",
                null
        );
    }


    private static ByteArrayResource buildResource(String filename, String content) {
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

}
