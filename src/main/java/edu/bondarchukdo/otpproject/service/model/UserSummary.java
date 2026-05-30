package edu.bondarchukdo.otpproject.service.model;

import edu.bondarchukdo.otpproject.domain.Role;

public record UserSummary(long id, String login, Role role, String email, String phone, String telegramChatId) {
}
