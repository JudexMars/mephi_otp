package edu.bondarchukdo.otpproject.service;

import java.security.SecureRandom;
import java.time.Instant;

import edu.bondarchukdo.otpproject.channel.OtpDeliveryCoordinator;
import edu.bondarchukdo.otpproject.dao.OtpCodeDao;
import edu.bondarchukdo.otpproject.dao.OtpConfigDao;
import edu.bondarchukdo.otpproject.dao.UserDao;
import edu.bondarchukdo.otpproject.domain.OtpCodeRecord;
import edu.bondarchukdo.otpproject.domain.OtpConfigRecord;
import edu.bondarchukdo.otpproject.domain.OtpStatus;
import edu.bondarchukdo.otpproject.domain.UserRecord;
import edu.bondarchukdo.otpproject.service.model.OtpGenerateCommand;
import edu.bondarchukdo.otpproject.service.model.OtpGenerateResult;
import edu.bondarchukdo.otpproject.service.model.OtpValidateCommand;
import edu.bondarchukdo.otpproject.service.model.OtpValidateResult;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserDao userDao;
    private final OtpConfigDao otpConfigDao;
    private final OtpCodeDao otpCodeDao;
    private final PasswordEncoder passwordEncoder;
    private final OtpDeliveryCoordinator deliveryCoordinator;

    public OtpService(
            UserDao userDao,
            OtpConfigDao otpConfigDao,
            OtpCodeDao otpCodeDao,
            PasswordEncoder passwordEncoder,
            OtpDeliveryCoordinator deliveryCoordinator) {
        this.userDao = userDao;
        this.otpConfigDao = otpConfigDao;
        this.otpCodeDao = otpCodeDao;
        this.passwordEncoder = passwordEncoder;
        this.deliveryCoordinator = deliveryCoordinator;
    }

    private static String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    @Transactional
    public OtpGenerateResult generate(long userId, OtpGenerateCommand command) {
        UserRecord user = userDao.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        OtpConfigRecord config =
                otpConfigDao.findSingleton().orElseThrow(() -> new IllegalArgumentException("OTP config missing"));

        String destination = OtpDestinationResolver.resolve(command, user);
        otpCodeDao.expireActiveForUserAndOperation(userId, command.operationId());

        String plainCode = randomDigits(config.codeLength());
        String hash = passwordEncoder.encode(plainCode);
        Instant expiresAt = Instant.now().plusSeconds(config.ttlSeconds());
        long otpId = otpCodeDao.insert(userId, command.operationId(), hash, OtpStatus.ACTIVE, expiresAt);

        try {
            deliveryCoordinator.deliver(
                    command.channel(), destination, plainCode, command.operationId(), user.login());
        } catch (RuntimeException e) {
            otpCodeDao.deleteById(otpId);
            throw e;
        }

        return new OtpGenerateResult(
                command.operationId(), command.channel(), "OTP generated and delivered successfully");
    }

    @Transactional
    public OtpValidateResult validate(long userId, OtpValidateCommand command) {
        OtpCodeRecord row = otpCodeDao
                .findActiveByUserAndOperation(userId, command.operationId())
                .orElse(null);
        if (row == null) {
            return new OtpValidateResult(false, "No active OTP for this operation");
        }
        Instant now = Instant.now();
        if (row.expiresAt().isBefore(now)) {
            otpCodeDao.updateStatus(row.id(), OtpStatus.EXPIRED);
            return new OtpValidateResult(false, "OTP expired");
        }
        if (!passwordEncoder.matches(command.code(), row.codeHash())) {
            return new OtpValidateResult(false, "Invalid code");
        }
        otpCodeDao.updateStatus(row.id(), OtpStatus.USED);
        return new OtpValidateResult(true, "OTP validated");
    }
}
