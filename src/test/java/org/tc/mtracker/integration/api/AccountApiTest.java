package org.tc.mtracker.integration.api;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.tc.mtracker.support.base.BaseApiIntegrationTest;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserRepository;

import java.math.BigDecimal;

@Tag("integration")
class AccountApiTest extends BaseApiIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldReturnDefaultAccount() {
        User user = fixtures.createUser("account@example.com", true, new BigDecimal("123.45"));

        restTestClient.get()
                .uri("/api/v1/accounts/default")
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(user.getDefaultAccount().getId())
                .jsonPath("$.balance").isEqualTo(123.45);
    }

    @Test
    void shouldReturnNotFoundWhenDefaultAccountIsMissing() {
        User user = fixtures.createUser("missing-account@example.com");
        user.setDefaultAccount(null);
        userRepository.saveAndFlush(user);

        restTestClient.get()
                .uri("/api/v1/accounts/default")
                .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                .exchange()
                .expectStatus().isNotFound();
    }
}
