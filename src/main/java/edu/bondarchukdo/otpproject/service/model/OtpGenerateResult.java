package edu.bondarchukdo.otpproject.service.model;

import edu.bondarchukdo.otpproject.domain.DeliveryChannel;

public record OtpGenerateResult(String operationId, DeliveryChannel channel, String message) {
}
