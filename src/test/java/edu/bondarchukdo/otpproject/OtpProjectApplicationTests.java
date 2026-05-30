package edu.bondarchukdo.otpproject;

import edu.bondarchukdo.otpproject.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OtpProjectApplicationTests extends AbstractPostgresIntegrationTest {

    @Test
    void contextLoads() {
        // Spring context + Flyway + PostgreSQL (Testcontainers)
    }
}
