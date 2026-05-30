package edu.bondarchukdo.otpproject.web;

import edu.bondarchukdo.otpproject.security.JwtPrincipal;
import edu.bondarchukdo.otpproject.service.OtpService;
import edu.bondarchukdo.otpproject.web.dto.OtpGenerateRequest;
import edu.bondarchukdo.otpproject.web.dto.OtpGenerateResponse;
import edu.bondarchukdo.otpproject.web.dto.OtpValidateRequest;
import edu.bondarchukdo.otpproject.web.dto.OtpValidateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/otp")
@Tag(name = "OTP", description = "Только роль USER")
@SecurityRequirement(name = "BearerAuth")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/generate")
    @Operation(
            summary = "Сгенерировать и отправить OTP",
            description =
                    """
                            Создаёт OTP для `operationId`, сохраняет хеш в БД и доставляет код выбранным каналом.
                            `destination` можно не указывать, если контакт задан при регистрации.
                            Для `FILE` код пишется в файл (см. `otp.file.path` в application.yml).
                            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP создан и отправлен"),
            @ApiResponse(responseCode = "400", description = "Нет destination / неверные параметры", content =
            @Content),
            @ApiResponse(responseCode = "502", description = "Ошибка канала доставки", content = @Content),
            @ApiResponse(responseCode = "403", description = "Нет роли USER", content = @Content)
    })
    public OtpGenerateResponse generateOtp(
            @AuthenticationPrincipal JwtPrincipal principal, @Valid @RequestBody OtpGenerateRequest request) {
        return WebMapper.toResponse(otpService.generate(principal.userId(), WebMapper.toCommand(request)));
    }

    @PostMapping("/validate")
    @Operation(summary = "Проверить OTP", description = "Сверяет код с активной записью для `operationId` и помечает " +
            "её как USED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Результат проверки в теле (`valid`: true/false)"),
            @ApiResponse(responseCode = "403", description = "Нет роли USER", content = @Content)
    })
    public OtpValidateResponse validateOtp(
            @AuthenticationPrincipal JwtPrincipal principal, @Valid @RequestBody OtpValidateRequest request) {
        return WebMapper.toResponse(otpService.validate(principal.userId(), WebMapper.toCommand(request)));
    }
}
