package edu.bondarchukdo.otpproject.web;

import java.util.List;

import edu.bondarchukdo.otpproject.service.model.AuthToken;
import edu.bondarchukdo.otpproject.service.model.LoginCredentials;
import edu.bondarchukdo.otpproject.service.model.OtpConfigCommand;
import edu.bondarchukdo.otpproject.service.model.OtpGenerateCommand;
import edu.bondarchukdo.otpproject.service.model.OtpGenerateResult;
import edu.bondarchukdo.otpproject.service.model.OtpValidateCommand;
import edu.bondarchukdo.otpproject.service.model.OtpValidateResult;
import edu.bondarchukdo.otpproject.service.model.RegisterCommand;
import edu.bondarchukdo.otpproject.service.model.UserSummary;
import edu.bondarchukdo.otpproject.web.dto.LoginRequest;
import edu.bondarchukdo.otpproject.web.dto.LoginResponse;
import edu.bondarchukdo.otpproject.web.dto.OtpConfigUpdateRequest;
import edu.bondarchukdo.otpproject.web.dto.OtpGenerateRequest;
import edu.bondarchukdo.otpproject.web.dto.OtpGenerateResponse;
import edu.bondarchukdo.otpproject.web.dto.OtpValidateRequest;
import edu.bondarchukdo.otpproject.web.dto.OtpValidateResponse;
import edu.bondarchukdo.otpproject.web.dto.RegisterRequest;
import edu.bondarchukdo.otpproject.web.dto.UserResponse;

public final class WebMapper {

    private WebMapper() {
    }

    public static RegisterCommand toCommand(RegisterRequest request) {
        return new RegisterCommand(
                request.login(),
                request.password(),
                request.role(),
                request.email(),
                request.phone(),
                request.telegramChatId());
    }

    public static LoginCredentials toCredentials(LoginRequest request) {
        return new LoginCredentials(request.login(), request.password());
    }

    public static LoginResponse toResponse(AuthToken token) {
        return new LoginResponse(token.accessToken(), token.expiresInSeconds());
    }

    public static OtpConfigCommand toCommand(OtpConfigUpdateRequest request) {
        return new OtpConfigCommand(request.ttlSeconds(), request.codeLength());
    }

    public static OtpGenerateCommand toCommand(OtpGenerateRequest request) {
        return new OtpGenerateCommand(request.operationId(), request.channel(), request.destination());
    }

    public static OtpValidateCommand toCommand(OtpValidateRequest request) {
        return new OtpValidateCommand(request.operationId(), request.code());
    }

    public static OtpGenerateResponse toResponse(OtpGenerateResult result) {
        return new OtpGenerateResponse(result.operationId(), result.channel().name(), result.message());
    }

    public static OtpValidateResponse toResponse(OtpValidateResult result) {
        return new OtpValidateResponse(result.valid(), result.message());
    }

    public static UserResponse toResponse(UserSummary summary) {
        return new UserResponse(
                summary.id(),
                summary.login(),
                summary.role(),
                summary.email(),
                summary.phone(),
                summary.telegramChatId());
    }

    public static List<UserResponse> toUserResponses(List<UserSummary> summaries) {
        return summaries.stream().map(WebMapper::toResponse).toList();
    }
}
