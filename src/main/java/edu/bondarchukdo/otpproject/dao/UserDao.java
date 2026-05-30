package edu.bondarchukdo.otpproject.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import edu.bondarchukdo.otpproject.domain.Role;
import edu.bondarchukdo.otpproject.domain.UserRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserDao {

    private static final RowMapper<UserRecord> ROW_MAPPER = (rs, rowNum) -> mapUser(rs);

    private final JdbcTemplate jdbcTemplate;

    public UserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static UserRecord mapUser(ResultSet rs) throws SQLException {
        return new UserRecord(
                rs.getLong("id"),
                rs.getString("login"),
                rs.getString("password_hash"),
                Role.valueOf(rs.getString("role")),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("telegram_chat_id"));
    }

    public boolean existsByLogin(String login) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE login = ?", Integer.class, login);
        return count != null && count > 0;
    }

    public boolean existsAdmin() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'", Integer.class);
        return count != null && count > 0;
    }

    public long insert(String login, String passwordHash, Role role, String email, String phone,
                       String telegramChatId) {
        Long id = jdbcTemplate.queryForObject(
                """
                        INSERT INTO users (login, password_hash, role, email, phone, telegram_chat_id)
                        VALUES (?, ?, ?, ?, ?, ?)
                        RETURNING id
                        """,
                Long.class,
                login,
                passwordHash,
                role.name(),
                email,
                phone,
                telegramChatId);
        return id != null ? id : 0L;
    }

    public Optional<UserRecord> findByLogin(String login) {
        List<UserRecord> list = jdbcTemplate.query(
                "SELECT id, login, password_hash, role, email, phone, telegram_chat_id FROM users WHERE login = ?",
                ROW_MAPPER,
                login);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    public Optional<UserRecord> findById(long id) {
        List<UserRecord> list = jdbcTemplate.query(
                "SELECT id, login, password_hash, role, email, phone, telegram_chat_id FROM users WHERE id = ?",
                ROW_MAPPER,
                id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    public List<UserRecord> findAllNonAdmins() {
        return jdbcTemplate.query(
                """
                        SELECT id, login, password_hash, role, email, phone, telegram_chat_id
                        FROM users WHERE role = 'USER' ORDER BY id
                        """,
                ROW_MAPPER);
    }

    public int deleteById(long id) {
        return jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
    }

    public boolean isAdmin(long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ? AND role = 'ADMIN'", Integer.class, userId);
        return count != null && count > 0;
    }
}
