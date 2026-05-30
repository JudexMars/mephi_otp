package edu.bondarchukdo.otpproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "otp.file")
public record OtpFileProperties(String path) {
}
