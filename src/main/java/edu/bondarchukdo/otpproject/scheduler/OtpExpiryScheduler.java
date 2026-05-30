package edu.bondarchukdo.otpproject.scheduler;

import java.time.Instant;

import edu.bondarchukdo.otpproject.dao.OtpCodeDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OtpExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(OtpExpiryScheduler.class);

    private final OtpCodeDao otpCodeDao;

    public OtpExpiryScheduler(OtpCodeDao otpCodeDao) {
        this.otpCodeDao = otpCodeDao;
    }

    @Scheduled(fixedDelayString = "${otp.expiry-scan-interval-ms:60000}")
    public void markExpired() {
        int updated = otpCodeDao.markExpiredBefore(Instant.now());
        if (updated > 0) {
            log.info("Marked {} OTP row(s) as EXPIRED", updated);
        }
    }
}
