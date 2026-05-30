package edu.bondarchukdo.otpproject.web;

import edu.bondarchukdo.otpproject.service.AuthService;
import edu.bondarchukdo.otpproject.web.dto.LoginRequest;
import edu.bondarchukdo.otpproject.web.dto.LoginResponse;
import edu.bondarchukdo.otpproject.web.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Регистрация и вход. JWT не требуется.")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Регистрация пользователя",
            description =
                    """
                            Создаёт учётную запись с ролью `USER` или `ADMIN`.
                            В системе может быть **не более одного** администратора.
                            Опционально можно указать контакты для каналов OTP: email, phone, telegramChatId.
                            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Пользователь создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации или логин занят / второй ADMIN",
                    content = @Content)
    })
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(WebMapper.toCommand(request));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Вход",
            description = "Проверяет логин и пароль, возвращает JWT (`accessToken`) для заголовка Authorization.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешная аутентификация"),
            @ApiResponse(responseCode = "400", description = "Неверные учётные данные", content = @Content)
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return WebMapper.toResponse(authService.login(WebMapper.toCredentials(request)));
    }
}
