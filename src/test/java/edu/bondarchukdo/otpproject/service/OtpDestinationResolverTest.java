package edu.bondarchukdo.otpproject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.bondarchukdo.otpproject.domain.DeliveryChannel;
import edu.bondarchukdo.otpproject.domain.Role;
import edu.bondarchukdo.otpproject.domain.UserRecord;
import edu.bondarchukdo.otpproject.service.model.OtpGenerateCommand;
import org.junit.jupiter.api.Test;

class OtpDestinationResolverTest {

    private static final UserRecord USER =
            new UserRecord(1L, "alice", "hash", Role.USER, "a@example.com", "79001234567", "12345");

    @Test
    void usesExplicitDestinationWhenProvided() {
        OtpGenerateCommand command =
                new OtpGenerateCommand("op-1", DeliveryChannel.SMS, " 79990001122 ");

        String destination = OtpDestinationResolver.resolve(command, USER);

        assertEquals("79990001122", destination);
    }

    @Test
    void resolvesEmailFromUserProfile() {
        OtpGenerateCommand command = new OtpGenerateCommand("op-1", DeliveryChannel.EMAIL, null);

        assertEquals("a@example.com", OtpDestinationResolver.resolve(command, USER));
    }

    @Test
    void resolvesPhoneFromUserProfile() {
        OtpGenerateCommand command = new OtpGenerateCommand("op-1", DeliveryChannel.SMS, null);

        assertEquals("79001234567", OtpDestinationResolver.resolve(command, USER));
    }

    @Test
    void fileChannelUsesPlaceholder() {
        OtpGenerateCommand command = new OtpGenerateCommand("op-1", DeliveryChannel.FILE, null);

        assertEquals("-", OtpDestinationResolver.resolve(command, USER));
    }

    @Test
    void rejectsMissingContactForChannel() {
        UserRecord withoutEmail = new UserRecord(1L, "alice", "hash", Role.USER, null, null, null);
        OtpGenerateCommand command = new OtpGenerateCommand("op-1", DeliveryChannel.EMAIL, null);

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> OtpDestinationResolver.resolve(command, withoutEmail));
        assertEquals("Missing email; update profile or pass destination", ex.getMessage());
    }
}
