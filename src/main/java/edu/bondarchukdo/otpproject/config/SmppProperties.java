package edu.bondarchukdo.otpproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smpp")
public record SmppProperties(
        String host,
        int port,
        String systemId,
        String password,
        String systemType,
        String sourceAddr) {
}
