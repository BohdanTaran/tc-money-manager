package org.tc.mtracker.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.env.Environment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.tc.mtracker.auth.dto.LoginRequestDto;
import org.tc.mtracker.auth.dto.ResetPasswordDTO;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Date;
import java.util.Map;

@AutoConfigureRestTestClient
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "security.jwt.secret-key=aZbYcXdWeVfUgThSiRjQkPlOmNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRrQqPpOoNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRrQqPpOoNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRrQqPpOoNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRrQqPpOoNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRr",
                "security.jwt.expiration-time=3600000"
        }
)
@Testcontainers
class AuthControllerTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.3.0");

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private Environment env;

    @MockitoBean
    private EmailService emailService;

    @Test
    @Sql("/datasets/test_users.sql")
    void shouldReturnAccessTokenIfUserIsLoggedSuccessfully() {
        LoginRequestDto authDto = new LoginRequestDto("test@gmail.com", "12345678");

        restTestClient
                .post()
                .uri("/api/v1/auth/login")
                .body(authDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty();
    }

    @Test
    @Sql("/datasets/test_users.sql")
    void shouldReturn401IfPasswordIsIncorrect() {
        LoginRequestDto authDto = new LoginRequestDto("test@gmail.com", "wrongpassword");

        restTestClient
                .post()
                .uri("/api/v1/auth/login")
                .body(authDto)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Invalid credentials. Password does not match!");
    }

    @Test
    @Sql("/datasets/test_users.sql")
    void shouldReturn401IfUserDoesNotExist() {
        LoginRequestDto authDto = new LoginRequestDto("nonexistent@gmail.com", "12345678");

        restTestClient
                .post()
                .uri("/api/v1/auth/login")
                .body(authDto)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("User with email nonexistent@gmail.com does not exist.");
    }

    @Test
    @Sql("/datasets/test_users.sql")
    void shouldSendResetTokenSuccessfully() {
        restTestClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/auth/getTokenToResetPassword")
                        .queryParam("email", "test@gmail.com")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("Your link to reset password was sent!");
    }

    @Test
    @Sql("/datasets/test_users.sql")
    void shouldReturn401WhenRequestingResetForNonExistentEmail() {
        restTestClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/auth/getTokenToResetPassword")
                        .queryParam("email", "nonexistent@gmail.com")
                        .build())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @Sql("/datasets/test_users.sql")
    void shouldResetPasswordSuccessfullyWithValidToken() {
        String validToken = generateTestToken("test@gmail.com", "password_reset", 60000);

        ResetPasswordDTO resetDto = new ResetPasswordDTO("newPassword123", "newPassword123");

        restTestClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/auth/reset-password/confirm")
                        .queryParam("token", validToken)
                        .build())
                .body(resetDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty();
    }

    @Test
    @Sql("/datasets/test_users.sql")
    void shouldReturn400WhenPasswordsDoNotMatch() {
        String validToken = generateTestToken("test@gmail.com", "password_reset", 60000);
        ResetPasswordDTO mismatchDto = new ResetPasswordDTO("newPassword123", "differentPassword");

        restTestClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/auth/reset-password/confirm")
                        .queryParam("token", validToken)
                        .build())
                .body(mismatchDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Sql("/datasets/test_users.sql")
    void shouldReturn401WhenTokenIsExpired() {
        String expiredToken = generateTestToken("test@gmail.com", "password_reset", -3600000);
        ResetPasswordDTO resetDto = new ResetPasswordDTO("newPassword123", "newPassword123");

        restTestClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/auth/reset-password/confirm")
                        .queryParam("token", expiredToken)
                        .build())
                .body(resetDto)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @Sql("/datasets/test_users.sql")
    void shouldReturn401WhenTokenPurposeIsWrong() {
        String wrongPurposeToken = generateTestToken("test@gmail.com", "email_verification", 60000);
        ResetPasswordDTO resetDto = new ResetPasswordDTO("newPassword123", "newPassword123");

        restTestClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/auth/reset-password/confirm")
                        .queryParam("token", wrongPurposeToken)
                        .build())
                .body(resetDto)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Helper to generate JWTs that match the Test configuration
     */
    private String generateTestToken(String email, String purpose, long expirationOffsetMs) {
        String secretKey = env.getProperty("security.jwt.secret-key");

        byte[] keyBytes = Decoders.BASE64.decode(secretKey);

        return Jwts.builder()
                .setClaims(Map.of("purpose", purpose))
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationOffsetMs))
                // 2. Use the decoded bytes to create the signing key
                .signWith(Keys.hmacShaKeyFor(keyBytes), SignatureAlgorithm.HS256)
                .compact();
    }
}