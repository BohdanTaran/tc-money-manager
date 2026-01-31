package org.tc.mtracker.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.tc.mtracker.auth.dto.LoginRequestDto;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
}