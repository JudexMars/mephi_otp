package edu.bondarchukdo.otpproject.channel;

import edu.bondarchukdo.otpproject.domain.DeliveryChannel;
import org.springframework.stereotype.Component;

@Component
public class OtpDeliveryCoordinator {

    private final EmailOtpSender emailOtpSender;
    private final SmsOtpSender smsOtpSender;
    private final TelegramOtpSender telegramOtpSender;
    private final FileOtpWriter fileOtpWriter;

    public OtpDeliveryCoordinator(
            EmailOtpSender emailOtpSender,
            SmsOtpSender smsOtpSender,
            TelegramOtpSender telegramOtpSender,
            FileOtpWriter fileOtpWriter) {
        this.emailOtpSender = emailOtpSender;
        this.smsOtpSender = smsOtpSender;
        this.telegramOtpSender = telegramOtpSender;
        this.fileOtpWriter = fileOtpWriter;
    }

    public void deliver(
            DeliveryChannel channel, String destination, String code, String operationId, String userLoginForTelegram) {
        switch (channel) {
            case EMAIL -> emailOtpSender.sendCode(destination, code);
            case SMS -> smsOtpSender.sendCode(destination, code);
            case TELEGRAM -> telegramOtpSender.sendCode(destination, userLoginForTelegram, code);
            case FILE -> fileOtpWriter.appendCode(operationId, code);
            default -> throw new IllegalStateException("Unexpected value: " + channel);
        }
    }
}
