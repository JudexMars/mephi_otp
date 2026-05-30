package edu.bondarchukdo.otpproject.channel;

import java.util.Properties;

import edu.bondarchukdo.otpproject.config.EmailProperties;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailOtpSender {

    private static final Logger log = LoggerFactory.getLogger(EmailOtpSender.class);

    private final EmailProperties emailProperties;
    private volatile Session session;

    public EmailOtpSender(EmailProperties emailProperties) {
        this.emailProperties = emailProperties;
    }

    private static String mask(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(email.indexOf('@'));
    }

    public void sendCode(String toEmail, String code) {
        Session mailSession = ensureSession();
        try {
            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(emailProperties.from()));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Your OTP Code");
            message.setText("Your verification code is: " + code);
            Transport.send(message);
            log.info("Email OTP sent to {}", mask(toEmail));
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send email", e);
        }
    }

    private Session ensureSession() {
        if (session != null) {
            return session;
        }
        synchronized (this) {
            if (session != null) {
                return session;
            }
            Properties config = emailProperties.toJavaMailProperties();
            session = Session.getInstance(
                    config,
                    new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(
                                    emailProperties.username(), emailProperties.password());
                        }
                    });
            return session;
        }
    }
}
