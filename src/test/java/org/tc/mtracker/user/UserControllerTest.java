package org.tc.mtracker.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.tc.mtracker.user.dto.UpdateUserProfileDTO;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.springframework.http.MediaType;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.tc.mtracker.utils.TestHelpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureRestTestClient
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "security.jwt.secret-key=aZbYcXdWeVfUgThSiRjQkPlOmNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRrQqPpOoNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRrQqPpOoNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRrQqPpOoNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRrQqPpOoNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRr",
                "security.jwt.expiration-time=3600000",
                "aws.region=us-central-1",
                "aws.s3.bucket-name=test-bucket",
                "aws.access-key-id=test-key-id",
                "aws.secret-access-key=test-secret"
        }
)
@Testcontainers
class UserControllerTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.3.0");

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestHelpers testHelpers;

    @Test
    @Sql("/datasets/test_users.sql")
    void shouldUpdateUsersFullnameSuccessfully() {
        String email = "test@gmail.com";
        String newFullname = "New Fullname";
        String token = testHelpers.generateTestToken(email, "access_token", 3600000);
        UpdateUserProfileDTO updateDto = new UpdateUserProfileDTO(newFullname);

        restTestClient
                .put()
                .uri("/api/v1/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("User updated successfully!");

        User updatedUser = userRepository.findByEmail(email).orElseThrow();

        assertThat(updatedUser.getFullName()).isEqualTo(newFullname);
    }

    @Test
    @Sql("/datasets/test_users.sql")
    void shouldReturn400WhenFullNameIsEmpty() {
        String email = "test@gmail.com";
        String newFullname = "";
        String token = testHelpers.generateTestToken(email, "access_token", 3600000);
        UpdateUserProfileDTO updateDto = new UpdateUserProfileDTO(newFullname);

        restTestClient
                .put()
                .uri("/api/v1/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Sql("/datasets/test_users.sql")
    void shouldReturn400WhenFullNameIsTooShort() {
        String token = testHelpers.generateTestToken("test@gmail.com", "access_token", 3600000);
        UpdateUserProfileDTO updateDto = new UpdateUserProfileDTO("");

        restTestClient
                .put()
                .uri("/api/v1/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Sql("/datasets/test_users.sql")
    void shouldReturn400WhenFullNameIsTooLong() {
        String token = testHelpers.generateTestToken("test@gmail.com", "access_token", 3600000);
        UpdateUserProfileDTO updateDto = new UpdateUserProfileDTO("a".repeat(129));

        restTestClient
                .put()
                .uri("/api/v1/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDto)
                .exchange()
                .expectStatus().isBadRequest();
    }


}