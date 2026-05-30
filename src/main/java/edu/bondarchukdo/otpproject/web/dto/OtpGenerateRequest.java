package edu.bondarchukdo.otpproject.web.dto;

import edu.bondarchukdo.otpproject.domain.DeliveryChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Генерация OTP и доставка по выбранному каналу")
public record OtpGenerateRequest(
        @Schema(description = "Идентификатор защищаемой операции", example = "payment-42") @NotBlank String operationId,
        @Schema(description = "Канал доставки", example = "FILE") @NotNull DeliveryChannel channel,
        @Schema(
                description =
                        "Адрес доставки (email / phone / chat_id). Необязателен, если задан в профиле при регистрации" +
                                ". Для FILE игнорируется.",
                example = "79001234567")
        String destination) {
}
