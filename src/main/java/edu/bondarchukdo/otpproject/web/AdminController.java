package edu.bondarchukdo.otpproject.web;

import java.util.List;

import edu.bondarchukdo.otpproject.service.AdminService;
import edu.bondarchukdo.otpproject.web.dto.OtpConfigUpdateRequest;
import edu.bondarchukdo.otpproject.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Только роль ADMIN")
@SecurityRequirement(name = "BearerAuth")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PutMapping("/otp-config")
    @Operation(summary = "Обновить настройки OTP", description = "Меняет TTL и длину кода в единственной строке " +
            "конфигурации.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Конфигурация обновлена"),
            @ApiResponse(responseCode = "403", description = "Нет роли ADMIN", content = @Content)
    })
    public ResponseEntity<Void> updateOtpConfig(@Valid @RequestBody OtpConfigUpdateRequest request) {
        adminService.updateOtpConfig(WebMapper.toCommand(request));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    @Operation(summary = "Список пользователей", description = "Возвращает всех пользователей с ролью USER (без " +
            "администраторов).")
    @ApiResponse(responseCode = "200", description = "Список пользователей")
    public List<UserResponse> listUsers() {
        return WebMapper.toUserResponses(adminService.listNonAdminUsers());
    }

    @DeleteMapping("/users/{id}")
    @Operation(
            summary = "Удалить пользователя",
            description = "Удаляет пользователя и связанные OTP-коды. Администратора удалить нельзя.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Пользователь удалён"),
            @ApiResponse(responseCode = "400", description = "Пользователь не найден или это ADMIN", content =
            @Content),
            @ApiResponse(responseCode = "403", description = "Нет роли ADMIN", content = @Content)
    })
    public ResponseEntity<Void> deleteUser(@PathVariable long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
