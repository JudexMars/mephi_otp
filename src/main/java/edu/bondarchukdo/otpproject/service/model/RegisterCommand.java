package edu.bondarchukdo.otpproject.service.model;

import edu.bondarchukdo.otpproject.domain.Role;

public record RegisterCommand(
        String login, String password, Role role, String email, String phone, String telegramChatId) {
}
