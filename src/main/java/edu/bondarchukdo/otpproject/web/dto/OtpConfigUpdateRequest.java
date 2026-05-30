package edu.bondarchukdo.otpproject.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OtpConfigUpdateRequest(
        @NotNull @Min(30) @Max(86400) Integer ttlSeconds,
        @NotNull @Min(4) @Max(12) Integer codeLength) {
}
