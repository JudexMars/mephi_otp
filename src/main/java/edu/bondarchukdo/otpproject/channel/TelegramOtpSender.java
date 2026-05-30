package edu.bondarchukdo.otpproject.channel;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import edu.bondarchukdo.otpproject.config.TelegramProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelegramOtpSender {

    private static final Logger log = LoggerFactory.getLogger(TelegramOtpSender.class);

    private final TelegramProperties telegramProperties;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public TelegramOtpSender(TelegramProperties telegramProperties) {
        this.telegramProperties = telegramProperties;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public void sendCode(String chatId, String destinationLabel, String code) {
        String token = telegramProperties.botToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("telegram.bot-token is missing in application.yml");
        }
        String message = String.format("%s, your confirmation code is: %s", destinationLabel, code);
        String url = String.format(
                "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s",
                token, chatId, urlEncode(message));

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Telegram API error. Status: " + response.statusCode() + " body: "
                        + response.body());
            }
            log.info("Telegram message sent to chat {}", chatId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling Telegram API", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call Telegram API", e);
        }
    }
}
