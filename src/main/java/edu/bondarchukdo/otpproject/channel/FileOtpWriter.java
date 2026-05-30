package edu.bondarchukdo.otpproject.channel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

import edu.bondarchukdo.otpproject.config.OtpFileProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FileOtpWriter {

    private static final Logger log = LoggerFactory.getLogger(FileOtpWriter.class);

    private final OtpFileProperties otpFileProperties;

    public FileOtpWriter(OtpFileProperties otpFileProperties) {
        this.otpFileProperties = otpFileProperties;
    }

    public void appendCode(String operationId, String code) {
        String pathStr = otpFileProperties.path();
        if (pathStr == null || pathStr.isBlank()) {
            pathStr = "./generated-otp.txt";
        }
        Path path = Path.of(pathStr).toAbsolutePath().normalize();
        try {
            Files.createDirectories(path.getParent() != null ? path.getParent() : Path.of("."));
            String line = Instant.now() + " operationId=" + operationId + " code=" + code + System.lineSeparator();
            Files.writeString(
                    path,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
            log.info("OTP written to file {}", path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write OTP file: " + path, e);
        }
    }
}
