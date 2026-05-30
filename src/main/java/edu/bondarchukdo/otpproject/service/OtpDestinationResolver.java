package edu.bondarchukdo.otpproject.service;

import edu.bondarchukdo.otpproject.domain.UserRecord;
import edu.bondarchukdo.otpproject.service.model.OtpGenerateCommand;

final class OtpDestinationResolver {

    private OtpDestinationResolver() {
    }

    static String resolve(OtpGenerateCommand command, UserRecord user) {
        if (command.destination() != null && !command.destination().isBlank()) {
            return command.destination().trim();
        }
        return switch (command.channel()) {
            case EMAIL -> requireContact(user.email(), "email");
            case SMS -> requireContact(user.phone(), "phone");
            case TELEGRAM -> requireContact(user.telegramChatId(), "telegram_chat_id");
            case FILE -> "-";
        };
    }

    private static String requireContact(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing " + field + "; update profile or pass destination");
        }
        return value.trim();
    }
}
