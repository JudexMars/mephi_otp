package edu.bondarchukdo.otpproject.web.dto;

import edu.bondarchukdo.otpproject.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Регистрация нового пользователя")
public record RegisterRequest(
        @Schema(description = "Уникальный логин", example = "alice") @NotBlank String login,
        @Schema(description = "Пароль (хранится как BCrypt-хеш)", example = "secret") @NotBlank String password,
        @Schema(description = "Роль: только один ADMIN в системе", example = "USER") @NotNull Role role,
        @Schema(description = "Email для канала EMAIL", example = "alice@example.com") String email,
        @Schema(description = "Телефон для канала SMS", example = "79001234567") String phone,
        @Schema(description = "Chat ID Telegram для канала TELEGRAM", example = "123456789") String telegramChatId) {
}
