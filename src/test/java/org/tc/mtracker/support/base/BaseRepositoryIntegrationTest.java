package org.tc.mtracker.support.base;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.tc.mtracker.TestcontainersConfiguration;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Sql(value = "/datasets/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class BaseRepositoryIntegrationTest {
}
