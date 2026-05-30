package edu.bondarchukdo.otpproject.dao;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import edu.bondarchukdo.otpproject.domain.OtpCodeRecord;
import edu.bondarchukdo.otpproject.domain.OtpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OtpCodeDao {

    private final JdbcTemplate jdbcTemplate;

    public OtpCodeDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insert(long userId, String operationId, String codeHash, OtpStatus status, Instant expiresAt) {
        Long id = jdbcTemplate.queryForObject(
                """
                        INSERT INTO otp_codes (user_id, operation_id, code_hash, status, expires_at)
                        VALUES (?, ?, ?, ?, ?)
                        RETURNING id
                        """,
                Long.class,
                userId,
                operationId,
                codeHash,
                status.name(),
                Timestamp.from(expiresAt));
        return id != null ? id : 0L;
    }

    public Optional<OtpCodeRecord> findActiveByUserAndOperation(long userId, String operationId) {
        List<OtpCodeRecord> rows = jdbcTemplate.query(
                """
                        SELECT id, user_id, operation_id, code_hash, status, expires_at, created_at
                        FROM otp_codes
                        WHERE user_id = ? AND operation_id = ? AND status = 'ACTIVE'
                        """,
                (rs, rowNum) -> new OtpCodeRecord(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("operation_id"),
                        rs.getString("code_hash"),
                        OtpStatus.valueOf(rs.getString("status")),
                        rs.getTimestamp("expires_at").toInstant(),
                        rs.getTimestamp("created_at").toInstant()),
                userId,
                operationId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    public int updateStatus(long id, OtpStatus status) {
        return jdbcTemplate.update("UPDATE otp_codes SET status = ? WHERE id = ?", status.name(), id);
    }

    public int markExpiredBefore(Instant now) {
        return jdbcTemplate.update(
                """
                        UPDATE otp_codes SET status = 'EXPIRED'
                        WHERE status = 'ACTIVE' AND expires_at < ?
                        """,
                Timestamp.from(now));
    }

    public int deleteByUserId(long userId) {
        return jdbcTemplate.update("DELETE FROM otp_codes WHERE user_id = ?", userId);
    }

    public int expireActiveForUserAndOperation(long userId, String operationId) {
        return jdbcTemplate.update(
                """
                        UPDATE otp_codes SET status = 'EXPIRED'
                        WHERE user_id = ? AND operation_id = ? AND status = 'ACTIVE'
                        """,
                userId,
                operationId);
    }

    public int deleteById(long id) {
        return jdbcTemplate.update("DELETE FROM otp_codes WHERE id = ?", id);
    }
}
