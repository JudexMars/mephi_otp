package edu.bondarchukdo.otpproject.web.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpValidateRequest(@NotBlank String operationId, @NotBlank String code) {
}
