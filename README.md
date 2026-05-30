# OTP Project

Backend service for protecting operations with time-limited one-time codes (OTP). Built with **Java 21**, **Spring Boot 4**, **PostgreSQL 17**, **JDBC** (no JPA), and **JWT** authentication.

## Modules (packages)

| Package | Responsibility |
|---------|----------------|
| `edu.bondarchukdo.otpproject.web` | REST controllers (`Auth`, `Otp`, `Admin`), DTOs, request logging, global exception handling |
| `edu.bondarchukdo.otpproject.service` | `AuthService`, `OtpService`, `AdminService`; service-layer command/result models |
| `edu.bondarchukdo.otpproject.dao` | JDBC access to `users`, `otp_config`, `otp_codes` |
| `edu.bondarchukdo.otpproject.domain` | Roles, OTP statuses, delivery channel enum, immutable records |
| `edu.bondarchukdo.otpproject.channel` | Email (SMTP), SMS (SMPP), Telegram Bot API, file append |
| `edu.bondarchukdo.otpproject.config` | Security, JWT properties, Spring configuration |
| `edu.bondarchukdo.otpproject.security` | JWT creation, parsing, servlet filter |
| `edu.bondarchukdo.otpproject.scheduler` | Periodic mark of expired OTP rows |

## Prerequisites

- JDK 21
- Docker (optional for `./gradlew test`: integration tests use Testcontainers and are skipped if Docker is unavailable)

**Colima / OrbStack (macOS):** the CLI may work while the JVM still does not see `/var/run/docker.sock`. Either start Colima with Docker socket in the default location, or export `DOCKER_HOST` before Gradle/IDE, for example:

`export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"`

(Use `colima status` or `ls ~/.colima` to confirm the actual path.) The Gradle `test` task also tries to set `DOCKER_HOST` automatically when that file exists and `/var/run/docker.sock` does not.
- PostgreSQL 17 for local runs (or use Docker Compose in the repo root)

## Quick start

1. Start infrastructure (PostgreSQL, Flyway migrations, SMPP simulator):

   ```bash
   docker compose up -d
   ```

   Check that migrations succeeded:

   ```bash
   docker compose logs flyway
   ```

   | Service | Image | Host ports | Purpose |
   |---------|-------|------------|---------|
   | `postgres` | `postgres:17-alpine` | `5432` | Application database |
   | `flyway` | `flyway/flyway:11-alpine` | — | Applies SQL from `src/main/resources/db/migration/` once Postgres is healthy |
   | `smpp-sim` | `bitsensedev/smpp-sim` | `2775` (SMPP), `8989` (web UI) | SMSC emulator for OTP over SMS |

   After adding or changing migration files, re-apply:

   ```bash
   docker compose up flyway
   ```

   SMPP credentials are preconfigured in [`application.yml`](src/main/resources/application.yml) (`smpp.*`) and match [`docker/smppsim/smppsim.props`](docker/smppsim/smppsim.props) mounted into the container. Defaults: `system-id` / `smppclient1`, `password` / `password`, host `localhost`, port `2775`.

   After changing `smppsim.props`, recreate the container: `docker compose up -d --force-recreate smpp-sim`. Successful start shows no `NullPointerException` in `docker logs smpp-sim`; web UI: http://localhost:8989 .

   SMPP PDU capture files (if enabled in `smppsim.props`) are stored in the Docker volume `smpp-sim-captures`, not in the project folder (avoids Colima bind-mount permission errors). Copy them out with:

   ```bash
   docker cp smpp-sim:/smppsim/captures ./smppsim-captures-export
   ```

2. Run the application:

   ```bash
   ./gradlew bootRun
   ```

   Default datasource: `jdbc:postgresql://localhost:5432/otpdb` user `otp` / password `otp` (see `application.yml`).

3. Configure delivery channels in [`application.yml`](src/main/resources/application.yml):

   - **Email:** `email.username`, `email.password`, `email.from`, `email.mail.smtp.*` (Angus Mail / SMTP).
   - **SMS:** `docker compose up -d smpp-sim` (included in full `docker compose up -d`). Settings under `smpp.*`. Web UI: http://localhost:8989 . Example OTP body: `"channel":"SMS"`, `"destination":"79001234567"`.
   - **Telegram:** create a bot with @BotFather, set `telegram.bot-token`. Use `getUpdates` to obtain `chat_id` and pass it as `destination` (or store `telegramChatId` at registration).

4. **OTP file path:** `otp.file.path` in `application.yml` (default `./generated-otp.txt` in the process working directory).

## API overview

Base path: `/api/v1`

### Public

| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/register` | Register user. Body: `login`, `password`, `role` (`USER` or `ADMIN`), optional `email`, `phone`, `telegramChatId`. Only **one** `ADMIN` may exist. |
| POST | `/auth/login` | Returns JSON: `accessToken`, `expiresInSeconds`. Header for protected calls: `Authorization: Bearer <token>`. |

### User (`ROLE_USER`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/otp/generate` | Body: `operationId`, `channel` (`EMAIL`, `SMS`, `TELEGRAM`, `FILE`), optional `destination`. Generates OTP from DB config, stores hash, delivers. If delivery fails, the new OTP row is removed. |
| POST | `/otp/validate` | Body: `operationId`, `code`. Marks OTP `USED` on success. |

### Administrator (`ROLE_ADMIN`)

| Method | Path | Description |
|--------|------|-------------|
| PUT | `/admin/otp-config` | Body: `ttlSeconds`, `codeLength`. Updates singleton OTP configuration. |
| GET | `/admin/users` | Lists all users with role `USER` (no administrators). |
| DELETE | `/admin/users/{id}` | Deletes user and related OTP codes (cannot delete an administrator). |

Non-admin JWTs receive **403** on `/admin/**`. Missing or invalid JWT on protected routes yields **401**/**403** per Spring Security.

## Swagger UI (OpenAPI)

После `./gradlew bootRun` откройте в браузере:

**http://localhost:8080/swagger-ui.html**

(альтернативный путь: http://localhost:8080/swagger-ui/index.html)

OpenAPI JSON: http://localhost:8080/v3/api-docs

### Как пользоваться

1. В разделе **Authentication** выполните `POST /api/v1/auth/register` (создайте `USER` и при необходимости одного `ADMIN`).
2. Выполните `POST /api/v1/auth/login`, скопируйте `accessToken` из ответа.
3. Нажмите **Authorize** (замок вверху справа).
4. Введите: `Bearer <ваш_accessToken>` — слово `Bearer`, пробел и сам токен без кавычек.
5. Вызывайте методы:
   - **OTP** — только с JWT пользователя (`USER`);
   - **Admin** — только с JWT администратора (`ADMIN`).

Пример тела для генерации OTP через файл (без внешних сервисов):

```json
{
  "operationId": "demo-1",
  "channel": "FILE"
}
```

## How to test

### Automated tests

```bash
./gradlew test
```

- With **Docker** (socket visible to the JVM, see Colima note above), Testcontainers starts PostgreSQL 17 and runs integration scenarios (single admin rule, FILE channel generate/validate, role separation, admin APIs).
- **Without Docker**, those tests are skipped (`@Testcontainers(disabledWithoutDocker = true)`).

### Manual smoke test (curl)

1. Register admin and user, login as user, generate with `FILE`, read the file line for `code=`, then validate (replace `TOKEN` and `CODE`):

   ```bash
   curl -s -X POST http://localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' \
     -d '{"login":"admin","password":"secret","role":"ADMIN"}'
   curl -s -X POST http://localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' \
     -d '{"login":"alice","password":"secret","role":"USER","email":"alice@example.com"}'
   TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
     -d '{"login":"alice","password":"secret"}' | jq -r .accessToken)
   curl -s -X POST http://localhost:8080/api/v1/otp/generate -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"operationId":"pay-1","channel":"FILE"}'
   # Inspect otp.file.path (default ./generated-otp.txt) for the plaintext line, then:
   curl -s -X POST http://localhost:8080/api/v1/otp/validate -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"operationId":"pay-1","code":"CODE"}'
   ```

2. Admin token: login as `admin`, then `curl` `GET /api/v1/admin/users` and `PUT /admin/otp-config` with the Bearer token.

### External libraries (Gradle)

Declared in `build.gradle`: Spring Boot WebMVC, JDBC, Security, Validation, Flyway, PostgreSQL driver, JJWT, Angus Mail, jsmpp, Testcontainers (tests). No extra manual install beyond Gradle sync.

## Security notes

- Change `jwt.secret` in production (HS256 requires a sufficiently long secret; see `application.yml`).
- Passwords are stored with BCrypt; OTP codes are stored as BCrypt hashes of the numeric code.

