package edu.bondarchukdo.otpproject.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import edu.bondarchukdo.otpproject.config.JwtProperties;
import edu.bondarchukdo.otpproject.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String CLAIM_ROLE = "role";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(long userId, String login, Role role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("login", login)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + properties.expirationMs()))
                .signWith(key)
                .compact();
    }

    public JwtPrincipal parseValid(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        long userId = Long.parseLong(claims.getSubject());
        String login = claims.get("login", String.class);
        Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
        return new JwtPrincipal(userId, login, role);
    }
}
