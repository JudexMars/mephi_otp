package edu.bondarchukdo.otpproject.service.model;

public record AuthToken(String accessToken, long expiresInSeconds) {
}
