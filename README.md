# OTP Project

Backend-сервис для защиты операций одноразовыми кодами (OTP) с ограниченным временем жизни. Реализован на **Java 21**, **Spring Boot 4**, **PostgreSQL 17**, **JDBC** (без JPA) и **JWT**-аутентификации.

## Модули (пакеты)

| Пакет | Назначение |
|-------|------------|
| `edu.bondarchukdo.otpproject.web` | REST-контроллеры (`Auth`, `Otp`, `Admin`), DTO, логирование запросов, глобальная обработка ошибок |
| `edu.bondarchukdo.otpproject.service` | `AuthService`, `OtpService`, `AdminService`; модели команд и результатов сервисного слоя |
| `edu.bondarchukdo.otpproject.dao` | JDBC-доступ к таблицам `users`, `otp_config`, `otp_codes` |
| `edu.bondarchukdo.otpproject.domain` | Роли, статусы OTP, каналы доставки, неизменяемые записи |
| `edu.bondarchukdo.otpproject.channel` | Email (SMTP), SMS (SMPP), Telegram Bot API, запись в файл |
| `edu.bondarchukdo.otpproject.config` | Security, свойства JWT, конфигурация Spring |
| `edu.bondarchukdo.otpproject.security` | Создание и разбор JWT, servlet-фильтр |
| `edu.bondarchukdo.otpproject.scheduler` | Периодическая пометка просроченных OTP |

## Требования

- JDK 21
- PostgreSQL 17 для локального запуска (или Docker Compose из корня репозитория)

## Быстрый старт

1. Запустите инфраструктуру (PostgreSQL, миграции Flyway, SMPP-эмулятор):

   ```bash
   docker compose up -d
   ```

   Убедитесь, что миграции прошли успешно:

   ```bash
   docker compose logs flyway
   ```

   | Сервис | Образ | Порты на хосте | Назначение |
   |--------|-------|----------------|------------|
   | `postgres` | `postgres:17-alpine` | `5432` | База данных приложения |
   | `flyway` | `flyway/flyway:11-alpine` | — | Применяет SQL из `src/main/resources/db/migration/` после готовности Postgres |
   | `smpp-sim` | `bitsensedev/smpp-sim` | `2775` (SMPP), `8989` (web UI) | Эмулятор SMSC для OTP по SMS |

   После добавления или изменения миграций выполните повторно:

   ```bash
   docker compose up flyway
   ```

   Учётные данные SMPP заданы в [`application.yml`](src/main/resources/application.yml) (`smpp.*`) и совпадают с [`docker/smppsim/smppsim.props`](docker/smppsim/smppsim.props), смонтированным в контейнер. По умолчанию: `system-id` / `smppclient1`, `password` / `password`, хост `localhost`, порт `2775`.

   После изменения `smppsim.props` пересоздайте контейнер: `docker compose up -d --force-recreate smpp-sim`. Успешный запуск — без `NullPointerException` в `docker logs smpp-sim`; web UI: http://localhost:8989 .

   Файлы захвата SMPP PDU (если включены в `smppsim.props`) хранятся в Docker-томе `smpp-sim-captures`, а не в папке проекта (избегает ошибок прав при bind-mount в Colima). Скопировать их можно так:

   ```bash
   docker cp smpp-sim:/smppsim/captures ./smppsim-captures-export
   ```

2. Запустите приложение:

   ```bash
   ./gradlew bootRun
   ```

   Datasource по умолчанию: `jdbc:postgresql://localhost:5432/otpdb`, пользователь `otp` / пароль `otp` (см. `application.yml`).

3. Настройте каналы доставки в [`application.yml`](src/main/resources/application.yml):

   - **Email:** `email.username`, `email.password`, `email.from`, `email.mail.smtp.*` (Angus Mail / SMTP).
   - **SMS:** `docker compose up -d smpp-sim` (входит в полный `docker compose up -d`). Параметры в секции `smpp.*`. Web UI: http://localhost:8989 . Пример тела OTP: `"channel":"SMS"`, `"destination":"79001234567"`.
   - **Telegram:** создайте бота через @BotFather, укажите `telegram.bot-token`. Через `getUpdates` получите `chat_id` и передайте его в `destination` (или сохраните `telegramChatId` при регистрации).

4. **Путь к файлу OTP:** `otp.file.path` в `application.yml` (по умолчанию `./generated-otp.txt` в рабочей директории процесса).

## Обзор API

Базовый путь: `/api/v1`

### Публичные методы

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/auth/register` | Регистрация пользователя. Тело: `login`, `password`, `role` (`USER` или `ADMIN`), опционально `email`, `phone`, `telegramChatId`. Допускается только **один** `ADMIN`. |
| POST | `/auth/login` | Возвращает JSON: `accessToken`, `expiresInSeconds`. Для защищённых вызовов: заголовок `Authorization: Bearer <token>`. |

### Пользователь (`ROLE_USER`)

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/otp/generate` | Тело: `operationId`, `channel` (`EMAIL`, `SMS`, `TELEGRAM`, `FILE`), опционально `destination`. Генерирует OTP по конфигу из БД, сохраняет хеш, доставляет код. При ошибке доставки новая запись OTP удаляется. |
| POST | `/otp/validate` | Тело: `operationId`, `code`. При успехе помечает OTP как `USED`. |

### Администратор (`ROLE_ADMIN`)

| Метод | Путь | Описание |
|-------|------|----------|
| PUT | `/admin/otp-config` | Тело: `ttlSeconds`, `codeLength`. Обновляет единственную запись конфигурации OTP. |
| GET | `/admin/users` | Список всех пользователей с ролью `USER` (без администраторов). |
| DELETE | `/admin/users/{id}` | Удаляет пользователя и связанные OTP-коды (администратора удалить нельзя). |

JWT без роли администратора получает **403** на `/admin/**`. Отсутствующий или невалидный JWT на защищённых маршрутах даёт **401**/**403** согласно Spring Security.

## Swagger UI (OpenAPI)

После `./gradlew bootRun` откройте в браузере:

**http://localhost:8081/swagger-ui.html**

(альтернативный путь: http://localhost:8081/swagger-ui/index.html)

OpenAPI JSON: http://localhost:8081/v3/api-docs

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

## Тестирование

### Автоматические тесты

```bash
./gradlew test
```

Юнит-тесты покрывают логику сервисного слоя с Mockito (Docker и база данных не нужны):

- `AuthServiceTest` — правила регистрации, вход
- `AdminServiceTest` — конфиг OTP, список и удаление пользователей
- `OtpServiceTest` — генерация/валидация OTP, откат при ошибке доставки
- `OtpDestinationResolverTest` — адреса доставки по каналам
- `JwtServiceTest` — создание и разбор токена

### Ручная проверка (curl)

1. Зарегистрируйте администратора и пользователя, войдите как пользователь, сгенерируйте OTP с каналом `FILE`, найдите в файле строку с `code=`, затем выполните валидацию (подставьте `TOKEN` и `CODE`):

   ```bash
   curl -s -X POST http://localhost:8081/api/v1/auth/register -H 'Content-Type: application/json' \
     -d '{"login":"admin","password":"secret","role":"ADMIN"}'
   curl -s -X POST http://localhost:8081/api/v1/auth/register -H 'Content-Type: application/json' \
     -d '{"login":"alice","password":"secret","role":"USER","email":"alice@example.com"}'
   TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/login -H 'Content-Type: application/json' \
     -d '{"login":"alice","password":"secret"}' | jq -r .accessToken)
   curl -s -X POST http://localhost:8081/api/v1/otp/generate -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"operationId":"pay-1","channel":"FILE"}'
   # Найдите код в otp.file.path (по умолчанию ./generated-otp.txt), затем:
   curl -s -X POST http://localhost:8081/api/v1/otp/validate -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"operationId":"pay-1","code":"CODE"}'
   ```

2. Токен администратора: войдите как `admin`, затем выполните `curl` для `GET /api/v1/admin/users` и `PUT /admin/otp-config` с Bearer-токеном.

### Внешние библиотеки (Gradle)

Объявлены в `build.gradle`: Spring Boot WebMVC, JDBC, Security, Validation, Flyway, драйвер PostgreSQL, JJWT, Angus Mail, jsmpp. Дополнительная ручная установка не требуется — достаточно синхронизации Gradle.

## Безопасность

- В production замените `jwt.secret` (для HS256 нужен достаточно длинный секрет; см. `application.yml`).
- Пароли хранятся с BCrypt; OTP-коды — в виде BCrypt-хешей числового кода.
