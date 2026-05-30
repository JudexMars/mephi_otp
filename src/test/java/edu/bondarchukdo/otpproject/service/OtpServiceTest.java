package edu.bondarchukdo.otpproject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.bondarchukdo.otpproject.channel.OtpDeliveryCoordinator;
import edu.bondarchukdo.otpproject.dao.OtpCodeDao;
import edu.bondarchukdo.otpproject.dao.OtpConfigDao;
import edu.bondarchukdo.otpproject.dao.UserDao;
import edu.bondarchukdo.otpproject.domain.DeliveryChannel;
import edu.bondarchukdo.otpproject.domain.OtpCodeRecord;
import edu.bondarchukdo.otpproject.domain.OtpConfigRecord;
import edu.bondarchukdo.otpproject.domain.OtpStatus;
import edu.bondarchukdo.otpproject.domain.Role;
import edu.bondarchukdo.otpproject.domain.UserRecord;
import edu.bondarchukdo.otpproject.service.model.OtpGenerateCommand;
import edu.bondarchukdo.otpproject.service.model.OtpGenerateResult;
import edu.bondarchukdo.otpproject.service.model.OtpValidateCommand;
import edu.bondarchukdo.otpproject.service.model.OtpValidateResult;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final UserRecord USER =
            new UserRecord(1L, "alice", "hash", Role.USER, null, null, null);

    @Mock
    private UserDao userDao;

    @Mock
    private OtpConfigDao otpConfigDao;

    @Mock
    private OtpCodeDao otpCodeDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpDeliveryCoordinator deliveryCoordinator;

    @InjectMocks
    private OtpService otpService;

    @Test
    void generateDeliversOtpViaFileChannel() {
        OtpGenerateCommand command = new OtpGenerateCommand("op-1", DeliveryChannel.FILE, null);
        when(userDao.findById(1L)).thenReturn(Optional.of(USER));
        when(otpConfigDao.findSingleton()).thenReturn(Optional.of(new OtpConfigRecord(300, 6)));
        when(passwordEncoder.encode(anyString())).thenReturn("otp-hash");
        when(otpCodeDao.insert(eq(1L), eq("op-1"), eq("otp-hash"), eq(OtpStatus.ACTIVE), any(Instant.class)))
                .thenReturn(10L);

        OtpGenerateResult result = otpService.generate(1L, command);

        assertEquals("op-1", result.operationId());
        assertEquals(DeliveryChannel.FILE, result.channel());
        verify(otpCodeDao).expireActiveForUserAndOperation(1L, "op-1");
        verify(deliveryCoordinator).deliver(eq(DeliveryChannel.FILE), eq("-"), anyString(), eq("op-1"), eq("alice"));
    }

    @Test
    void generateRollsBackWhenDeliveryFails() {
        OtpGenerateCommand command = new OtpGenerateCommand("op-1", DeliveryChannel.FILE, null);
        when(userDao.findById(1L)).thenReturn(Optional.of(USER));
        when(otpConfigDao.findSingleton()).thenReturn(Optional.of(new OtpConfigRecord(300, 6)));
        when(passwordEncoder.encode(anyString())).thenReturn("otp-hash");
        when(otpCodeDao.insert(anyLong(), anyString(), anyString(), any(), any())).thenReturn(10L);
        doThrow(new IllegalStateException("smtp down"))
                .when(deliveryCoordinator)
                .deliver(any(), anyString(), anyString(), anyString(), anyString());

        assertThrows(IllegalStateException.class, () -> otpService.generate(1L, command));

        verify(otpCodeDao).deleteById(10L);
    }

    @Test
    void validateMarksOtpAsUsedWhenCodeMatches() {
        Instant expiresAt = Instant.now().plusSeconds(300);
        OtpCodeRecord row = new OtpCodeRecord(5L, 1L, "op-1", "hash", OtpStatus.ACTIVE, expiresAt, Instant.now());
        when(otpCodeDao.findActiveByUserAndOperation(1L, "op-1")).thenReturn(Optional.of(row));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

        OtpValidateResult result = otpService.validate(1L, new OtpValidateCommand("op-1", "123456"));

        assertTrue(result.valid());
        verify(otpCodeDao).updateStatus(5L, OtpStatus.USED);
    }

    @Test
    void validateRejectsWrongCode() {
        Instant expiresAt = Instant.now().plusSeconds(300);
        OtpCodeRecord row = new OtpCodeRecord(5L, 1L, "op-1", "hash", OtpStatus.ACTIVE, expiresAt, Instant.now());
        when(otpCodeDao.findActiveByUserAndOperation(1L, "op-1")).thenReturn(Optional.of(row));
        when(passwordEncoder.matches("000000", "hash")).thenReturn(false);

        OtpValidateResult result = otpService.validate(1L, new OtpValidateCommand("op-1", "000000"));

        assertFalse(result.valid());
        assertEquals("Invalid code", result.message());
        verify(otpCodeDao, never()).updateStatus(anyLong(), eq(OtpStatus.USED));
    }

    @Test
    void validateMarksExpiredOtp() {
        Instant expiresAt = Instant.now().minusSeconds(1);
        OtpCodeRecord row = new OtpCodeRecord(5L, 1L, "op-1", "hash", OtpStatus.ACTIVE, expiresAt, Instant.now());
        when(otpCodeDao.findActiveByUserAndOperation(1L, "op-1")).thenReturn(Optional.of(row));

        OtpValidateResult result = otpService.validate(1L, new OtpValidateCommand("op-1", "123456"));

        assertFalse(result.valid());
        assertEquals("OTP expired", result.message());
        verify(otpCodeDao).updateStatus(5L, OtpStatus.EXPIRED);
    }

    @Test
    void validateReturnsFalseWhenNoActiveOtp() {
        when(otpCodeDao.findActiveByUserAndOperation(1L, "op-1")).thenReturn(Optional.empty());

        OtpValidateResult result = otpService.validate(1L, new OtpValidateCommand("op-1", "123456"));

        assertFalse(result.valid());
        assertEquals("No active OTP for this operation", result.message());
    }
}
