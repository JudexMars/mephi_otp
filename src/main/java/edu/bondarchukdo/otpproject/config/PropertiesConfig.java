package edu.bondarchukdo.otpproject.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        OtpFileProperties.class,
        SmppProperties.class,
        EmailProperties.class,
        TelegramProperties.class
})
public class PropertiesConfig {
}
