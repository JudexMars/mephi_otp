package edu.bondarchukdo.otpproject.domain;

import java.time.Instant;

public record OtpCodeRecord(
        long id,
        long userId,
        String operationId,
        String codeHash,
        OtpStatus status,
        Instant expiresAt,
        Instant createdAt
) {
}
