package edu.bondarchukdo.otpproject.domain;

public record UserRecord(
        long id,
        String login,
        String passwordHash,
        Role role,
        String email,
        String phone,
        String telegramChatId
) {
}
