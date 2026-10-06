package com.meterly;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for tests that need the full app + a real database.
 * disabledWithoutDocker = true: if Docker isn't running, these tests are SKIPPED instead of failing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = "meterly.demo-data.enabled=false")
public abstract class IntegrationTest {
}
