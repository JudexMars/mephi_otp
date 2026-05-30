package edu.bondarchukdo.otpproject.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ входа с JWT")
public record LoginResponse(
        @Schema(description = "JWT для заголовка Authorization: Bearer …", example = "eyJhbGciOiJIUzI1NiIs…")
        String accessToken,
        @Schema(description = "Срок действия токена в секундах", example = "86400") long expiresInSeconds) {
}
