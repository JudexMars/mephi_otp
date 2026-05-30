package edu.bondarchukdo.otpproject.service.model;

public record OtpConfigCommand(int ttlSeconds, int codeLength) {
}
