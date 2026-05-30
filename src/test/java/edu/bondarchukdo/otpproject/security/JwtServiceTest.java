package edu.bondarchukdo.otpproject.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.bondarchukdo.otpproject.config.JwtProperties;
import edu.bondarchukdo.otpproject.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties =
                new JwtProperties("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", 3_600_000L);
        jwtService = new JwtService(properties);
    }

    @Test
    void createTokenCanBeParsedBack() {
        String token = jwtService.createToken(42L, "alice", Role.USER);

        JwtPrincipal principal = jwtService.parseValid(token);

        assertEquals(42L, principal.userId());
        assertEquals("alice", principal.login());
        assertEquals(Role.USER, principal.role());
    }
}
