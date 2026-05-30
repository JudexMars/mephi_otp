package edu.bondarchukdo.otpproject.service.model;

import edu.bondarchukdo.otpproject.domain.DeliveryChannel;

public record OtpGenerateCommand(String operationId, DeliveryChannel channel, String destination) {
}
