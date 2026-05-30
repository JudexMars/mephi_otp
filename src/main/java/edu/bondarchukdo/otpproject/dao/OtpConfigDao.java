package edu.bondarchukdo.otpproject.dao;

import java.util.Optional;

import edu.bondarchukdo.otpproject.domain.OtpConfigRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OtpConfigDao {

    private final JdbcTemplate jdbcTemplate;

    public OtpConfigDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<OtpConfigRecord> findSingleton() {
        return jdbcTemplate
                .query(
                        "SELECT ttl_seconds, code_length FROM otp_config WHERE id = 1",
                        (rs, rowNum) -> new OtpConfigRecord(rs.getInt("ttl_seconds"), rs.getInt("code_length")))
                .stream()
                .findFirst();
    }

    public void upsert(int ttlSeconds, int codeLength) {
        jdbcTemplate.update(
                """
                        INSERT INTO otp_config (id, ttl_seconds, code_length)
                        VALUES (1, ?, ?)
                        ON CONFLICT (id) DO UPDATE SET ttl_seconds = EXCLUDED.ttl_seconds, code_length = EXCLUDED.code_length
                        """,
                ttlSeconds,
                codeLength);
    }
}
