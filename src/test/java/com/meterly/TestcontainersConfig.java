package com.meterly;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Starts a throwaway PostgreSQL 17 in Docker for tests.
 * @ServiceConnection tells Spring Boot to point spring.datasource.* at this container automatically.
 * As a Spring bean, the container lives as long as the (cached) test context: started once, reused.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:17");
    }
}
