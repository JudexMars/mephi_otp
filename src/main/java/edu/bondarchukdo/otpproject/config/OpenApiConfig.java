package edu.bondarchukdo.otpproject.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI otpOpenApi(@Value("${server.port:8081}") int serverPort) {
        final String bearerScheme = "BearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("OTP Service API")
                        .version("1.0.0")
                        .description(
                                """
                                        REST API сервиса одноразовых кодов (OTP) для подтверждения операций.
                                        
                                        ## Быстрый старт в Swagger UI
                                        1. **POST /api/v1/auth/register** — создайте пользователя (`USER`) и при необходимости одного администратора (`ADMIN`).
                                        2. **POST /api/v1/auth/login** — получите `accessToken`.
                                        3. Нажмите **Authorize** (вверху справа), введите: `Bearer <accessToken>` (слово Bearer и пробел обязательны).
                                        4. Вызывайте защищённые методы:
                                           - роль **USER** → `/api/v1/otp/*`
                                           - роль **ADMIN** → `/api/v1/admin/*`
                                        
                                        ## Каналы доставки OTP
                                        `EMAIL`, `SMS` (SMPP-симулятор), `TELEGRAM`, `FILE` (запись в файл на сервере).
                                        
                                        ## Инфраструктура
                                        PostgreSQL и Flyway — через `docker compose up -d`; SMPP — сервис `smpp-sim` на порту 2775.
                                        Все настройки приложения — в `application.yml` (email, telegram, smpp, jwt, otp).
                                        """)
                        .contact(new Contact().name("OTP Project").email("support@example.com"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .addServersItem(new Server().url("http://localhost:" + serverPort).description("Local"))
                .components(new Components()
                        .addSecuritySchemes(
                                bearerScheme,
                                new SecurityScheme()
                                        .name(bearerScheme)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "JWT из ответа POST /api/v1/auth/login. В Swagger UI: Authorize → " +
                                                        "`Bearer eyJhbG...`")));
    }
}
