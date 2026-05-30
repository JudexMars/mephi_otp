package edu.bondarchukdo.otpproject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.bondarchukdo.otpproject.support.AbstractPostgresIntegrationTest;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OtpApplicationIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final Pattern OTP_LINE = Pattern.compile("code=(\\d+)");

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("DELETE FROM otp_config");
        jdbcTemplate.execute("INSERT INTO otp_config (id, ttl_seconds, code_length) VALUES (1, 300, 6)");
    }

    private String url(String path) {
        return "http://127.0.0.1:" + port + path;
    }

    private HttpResponse<String> postJson(String path, String json, String bearerToken)
            throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url(path)))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        if (bearerToken != null) {
            b.header("Authorization", "Bearer " + bearerToken);
        }
        return httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> get(String path, String bearerToken) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url(path)))
                .timeout(Duration.ofSeconds(30))
                .GET();
        if (bearerToken != null) {
            b.header("Authorization", "Bearer " + bearerToken);
        }
        return httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> delete(String path, String bearerToken) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url(path)))
                .timeout(Duration.ofSeconds(30))
                .DELETE();
        if (bearerToken != null) {
            b.header("Authorization", "Bearer " + bearerToken);
        }
        return httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> putJson(String path, String json, String bearerToken)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url(path)))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + bearerToken)
                .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @Test
    void secondAdminRegistrationRejected() throws Exception {
        HttpResponse<String> first = postJson("/api/v1/auth/register", """
                {"login":"a1","password":"p1","role":"ADMIN"}
                """, null);
        assertEquals(201, first.statusCode());

        HttpResponse<String> second = postJson("/api/v1/auth/register", """
                {"login":"a2","password":"p2","role":"ADMIN"}
                """, null);
        assertEquals(400, second.statusCode());
        assertTrue(objectMapper.readTree(second.body()).has("error"));
    }

    @Test
    void fileChannelGenerateAndValidate() throws Exception {
        assertEquals(
                201,
                postJson(
                                "/api/v1/auth/register",
                                """
                                {"login":"admin2","password":"secret","role":"ADMIN"}
                                """,
                                null)
                        .statusCode());

        assertEquals(
                201,
                postJson(
                                "/api/v1/auth/register",
                                """
                                {"login":"u1","password":"secret","role":"USER"}
                                """,
                                null)
                        .statusCode());

        HttpResponse<String> login =
                postJson("/api/v1/auth/login", """
                {"login":"u1","password":"secret"}
                """, null);
        assertEquals(200, login.statusCode());
        String token = objectMapper.readTree(login.body()).get("accessToken").asText();

        HttpResponse<String> gen = postJson(
                "/api/v1/otp/generate",
                """
                {"operationId":"op-1","channel":"FILE"}
                """,
                token);
        assertEquals(200, gen.statusCode());

        Path otpFile = Path.of("build", "otp-integration-test.txt").toAbsolutePath();
        List<String> lines = Files.readAllLines(otpFile, StandardCharsets.UTF_8);
        String lastLine = lines.get(lines.size() - 1);
        Matcher m = OTP_LINE.matcher(lastLine);
        assertTrue(m.find(), "OTP line not found in file: " + lastLine);
        String code = m.group(1);

        HttpResponse<String> val = postJson(
                "/api/v1/otp/validate",
                String.format(
                        """
                        {"operationId":"op-1","code":"%s"}
                        """,
                        code),
                token);
        assertEquals(200, val.statusCode());
        assertTrue(objectMapper.readTree(val.body()).get("valid").asBoolean());
    }

    @Test
    void userCannotCallAdminApi() throws Exception {
        postJson(
                "/api/v1/auth/register",
                """
                {"login":"adm3","password":"secret","role":"ADMIN"}
                """,
                null);
        postJson(
                "/api/v1/auth/register",
                """
                {"login":"u2","password":"secret","role":"USER"}
                """,
                null);

        HttpResponse<String> login =
                postJson("/api/v1/auth/login", """
                {"login":"u2","password":"secret"}
                """, null);
        String token = objectMapper.readTree(login.body()).get("accessToken").asText();

        HttpResponse<String> blocked = get("/api/v1/admin/users", token);
        assertEquals(403, blocked.statusCode());
    }

    @Test
    void adminCanListUsersAndUpdateConfig() throws Exception {
        postJson(
                "/api/v1/auth/register",
                """
                {"login":"adm4","password":"secret","role":"ADMIN"}
                """,
                null);
        postJson(
                "/api/v1/auth/register",
                """
                {"login":"u3","password":"secret","role":"USER","email":"u3@example.com"}
                """,
                null);

        HttpResponse<String> login =
                postJson("/api/v1/auth/login", """
                {"login":"adm4","password":"secret"}
                """, null);
        String token = objectMapper.readTree(login.body()).get("accessToken").asText();

        HttpResponse<String> users = get("/api/v1/admin/users", token);
        assertEquals(200, users.statusCode());
        assertEquals("u3", objectMapper.readTree(users.body()).get(0).get("login").asText());

        HttpResponse<String> put = putJson(
                "/api/v1/admin/otp-config",
                """
                {"ttlSeconds":120,"codeLength":6}
                """,
                token);
        assertEquals(204, put.statusCode());

        HttpResponse<String> userLogin =
                postJson("/api/v1/auth/login", """
                {"login":"u3","password":"secret"}
                """, null);
        String userToken = objectMapper.readTree(userLogin.body()).get("accessToken").asText();

        HttpResponse<String> forbiddenDelete = delete("/api/v1/admin/users/999999", userToken);
        assertEquals(403, forbiddenDelete.statusCode());
    }
}
