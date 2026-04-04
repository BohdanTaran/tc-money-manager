package org.tc.mtracker.support.base;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.tc.mtracker.TestcontainersConfiguration;
import org.tc.mtracker.auth.mail.AuthEmailService;
import org.tc.mtracker.support.factory.DatabaseTestDataFactory;
import org.tc.mtracker.support.factory.JwtTestTokenFactory;
import org.tc.mtracker.user.User;
import org.tc.mtracker.utils.S3Service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@AutoConfigureRestTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Sql(value = "/datasets/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class BaseApiIntegrationTest {

    @Autowired
    protected RestTestClient restTestClient;

    @Autowired
    protected DatabaseTestDataFactory fixtures;

    @Autowired
    protected JwtTestTokenFactory jwtFactory;

    @MockitoBean
    protected S3Service s3Service;

    @MockitoBean
    protected AuthEmailService authEmailService;

    @BeforeEach
    void stubGeneratedUrls() {
        lenient().when(s3Service.generatePresignedUrl(anyString()))
                .thenAnswer(invocation -> "https://test-bucket.local/" + invocation.getArgument(0));
    }

    protected String authHeader(User user) {
        return jwtFactory.bearerAccessToken(user);
    }

    protected String authHeader(String email) {
        return jwtFactory.bearerAccessToken(email);
    }
}
