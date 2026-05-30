package edu.bondarchukdo.otpproject.support;

import java.nio.file.Path;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractPostgresIntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add(
                "jwt.secret",
                () -> "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        registry.add("jwt.expiration-ms", () -> "3600000");
        registry.add(
                "otp.file.path",
                () -> Path.of("build", "otp-integration-test.txt").toAbsolutePath().toString());
    }
}
