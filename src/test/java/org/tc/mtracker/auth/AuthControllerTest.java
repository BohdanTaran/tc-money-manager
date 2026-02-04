package org.tc.mtracker.auth;

import org.junit.jupiter.api.Nested;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.auth.dto.AuthRequestDTO;
import org.tc.mtracker.auth.dto.LoginRequestDto;
import org.tc.mtracker.image.S3Service;
import org.tc.mtracker.auth.dto.ResetPasswordDTO;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.Map;

@AutoConfigureRestTestClient
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "security.jwt.secret-key=aZbYcXdWeVfUgThSiRjQkPlOmNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRrQqPpOoNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRrQqPpOoNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRrQqPpOoNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRrQqPpOoNnMmLlKkJjIiHhGgFfEeDdCcBbAaZyXxWwVvUuTtSsRr",
                "security.jwt.expiration-time=3600000",
                "aws.region=us-central-1",
                "aws.bucket-name=test-bucket",
                "aws.access-key-id=test-key-id",
                "aws.secret-access-key=test-secret"

        }
)
@Testcontainers
class AuthControllerTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.3.0");

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private S3Service s3Service;

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private Environment env;


    @Nested
    class SignUpTests {
        @Test
        void shouldReturn201AndAuthResponseDtoIfUserIsSignedUpSuccessfullyWithoutAvatar() {
            MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
            multipartBodyBuilder.part("dto", new AuthRequestDTO(
                    "new1-user@gmail.com",
                    "12345678",
                    "New1 User",
                    "USD"
            ), MediaType.APPLICATION_JSON);

            restTestClient
                    .post()
                    .uri("/api/v1/auth/sign-up")
                    .body(multipartBodyBuilder.build())
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.id").isNotEmpty()
                    .jsonPath("$.fullName").isEqualTo("New1 User")
                    .jsonPath("$.email").isEqualTo("new1-user@gmail.com")
                    .jsonPath("$.currencyCode").isEqualTo("USD")
                    .jsonPath("$.avatarUrl").isEmpty()
                    .jsonPath("$.isActivated").isEqualTo(false);

            verifyNoInteractions(s3Service);
            verify(emailService, times(1)).sendVerificationEmail(any());
            verifyNoMoreInteractions(emailService);
        }

        @Test
        void shouldReturn201AndAuthResponseDtoIfUserIsSignedUpSuccessfullyWithAvatar() {
            when(s3Service.generatePresignedUrl(any(String.class))).thenReturn("test-avatar-url");

            MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
            multipartBodyBuilder.part("dto", new AuthRequestDTO(
                    "new-user@gmail.com",
                    "12345678",
                    "New User",
                    "USD"
            ), MediaType.APPLICATION_JSON);

            byte[] avatarBytes = "test-avatar.jpg".getBytes();
            ByteArrayResource avatarResource = new ByteArrayResource(avatarBytes) {
                @Override
                public String getFilename() {
                    return "test-avatar.jpg";
                }
            };
            multipartBodyBuilder.part("avatar", avatarResource, MediaType.IMAGE_JPEG);

            restTestClient
                    .post()
                    .uri("/api/v1/auth/sign-up")
                    .body(multipartBodyBuilder.build())
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.id").isNotEmpty()
                    .jsonPath("$.fullName").isEqualTo("New User")
                    .jsonPath("$.email").isEqualTo("new-user@gmail.com")
                    .jsonPath("$.currencyCode").isEqualTo("USD")
                    .jsonPath("$.avatarUrl").isEqualTo("test-avatar-url")
                    .jsonPath("$.isActivated").isEqualTo(false);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

            verify(s3Service, times(1)).saveFile(keyCaptor.capture(), any(MultipartFile.class));
            verify(s3Service, times(1)).generatePresignedUrl(keyCaptor.getValue());
            verify(emailService, times(1)).sendVerificationEmail(any());
            verifyNoMoreInteractions(s3Service, emailService);
        }

        @Test
        void shouldReturn400IfDtoPartIsMissingOnSignUp() {
            MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();

            byte[] avatarBytes = "avatar".getBytes();
            ByteArrayResource avatarResource = new ByteArrayResource(avatarBytes) {
                @Override
                public String getFilename() {
                    return "test-avatar.jpg";
                }
            };
            multipartBodyBuilder.part("avatar", avatarResource, MediaType.IMAGE_JPEG);

            restTestClient
                    .post()
                    .uri("/api/v1/auth/sign-up")
                    .body(multipartBodyBuilder.build())
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody();

            verifyNoInteractions(s3Service, emailService);
        }

        @Test
        void shouldReturn400IfAvatarHasUnsupportedContentTypeOnSignUp() {
            MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
            multipartBodyBuilder.part("dto", new AuthRequestDTO(
                    "new-user3@gmail.com",
                    "12345678",
                    "New User",
                    "USD"
            ), MediaType.APPLICATION_JSON);

            byte[] avatarBytes = "not-an-image".getBytes();
            ByteArrayResource avatarResource = new ByteArrayResource(avatarBytes) {
                @Override
                public String getFilename() {
                    return "avatar.txt";
                }
            };
            multipartBodyBuilder.part("avatar", avatarResource, MediaType.TEXT_PLAIN);

            restTestClient
                    .post()
                    .uri("/api/v1/auth/sign-up")
                    .body(multipartBodyBuilder.build())
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody();

            verifyNoInteractions(s3Service, emailService);
        }
    }

    @Nested
    class LoginTests {
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
                .signWith(Keys.hmacShaKeyFor(keyBytes), SignatureAlgorithm.HS256)
                .compact();
    }
}