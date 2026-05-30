CREATE TABLE users
(
    id               BIGSERIAL PRIMARY KEY,
    login            VARCHAR(128) NOT NULL UNIQUE,
    password_hash    VARCHAR(255) NOT NULL,
    role             VARCHAR(32)  NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    email            VARCHAR(255),
    phone            VARCHAR(64),
    telegram_chat_id VARCHAR(64)
);

CREATE TABLE otp_config
(
    id          SMALLINT PRIMARY KEY CHECK (id = 1),
    ttl_seconds INT NOT NULL CHECK (ttl_seconds > 0),
    code_length INT NOT NULL CHECK (code_length >= 4 AND code_length <= 12)
);

INSERT INTO otp_config (id, ttl_seconds, code_length)
VALUES (1, 300, 6);

CREATE TABLE otp_codes
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    operation_id VARCHAR(256) NOT NULL,
    code_hash    VARCHAR(255) NOT NULL,
    status       VARCHAR(32)  NOT NULL CHECK (status IN ('ACTIVE', 'EXPIRED', 'USED')),
    expires_at   TIMESTAMPTZ  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_otp_codes_user ON otp_codes (user_id);
CREATE INDEX idx_otp_codes_operation ON otp_codes (operation_id);
CREATE INDEX idx_otp_codes_status ON otp_codes (status);
CREATE INDEX idx_otp_codes_expires ON otp_codes (expires_at) WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX uq_otp_user_operation_active ON otp_codes (user_id, operation_id) WHERE status = 'ACTIVE';
