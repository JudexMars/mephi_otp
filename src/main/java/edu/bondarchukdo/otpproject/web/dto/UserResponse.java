package edu.bondarchukdo.otpproject.web.dto;

import edu.bondarchukdo.otpproject.domain.Role;

public record UserResponse(long id, String login, Role role, String email, String phone, String telegramChatId) {
}
