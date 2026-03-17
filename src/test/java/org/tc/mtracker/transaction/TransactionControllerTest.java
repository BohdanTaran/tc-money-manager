package org.tc.mtracker.transaction;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.tc.mtracker.transaction.dto.TransactionCreateRequestDTO;

import java.math.BigDecimal;
import java.time.LocalDate;

@AutoConfigureRestTestClient
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class TransactionControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void shouldReturn201WhenTransactionIsCreated() {
        TransactionCreateRequestDTO dto = new TransactionCreateRequestDTO(
                BigDecimal.valueOf(1.0), TransactionType.INCOME, 1L
                , LocalDate.now(), "Shop");

        restTestClient.post()
                .uri("/api/v1/transactions")
                .body(dto)
                .exchange()
                .expectStatus().isCreated();
    }
}
