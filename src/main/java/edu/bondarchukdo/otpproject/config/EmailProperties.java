package edu.bondarchukdo.otpproject.config;

import java.util.Properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "email")
public record EmailProperties(String username, String password, String from, Mail mail) {

    public Properties toJavaMailProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.host", mail.smtp().host());
        props.put("mail.smtp.port", String.valueOf(mail.smtp().port()));
        props.put("mail.smtp.auth", String.valueOf(mail.smtp().auth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(mail.smtp().starttls().enable()));
        return props;
    }

    public record Mail(Smtp smtp) {
    }

    public record Smtp(String host, int port, boolean auth, Starttls starttls) {
    }

    public record Starttls(boolean enable) {
    }
}
