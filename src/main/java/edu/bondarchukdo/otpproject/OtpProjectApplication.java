package edu.bondarchukdo.otpproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OtpProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(OtpProjectApplication.class, args);
    }
}
