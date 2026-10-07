CREATE TABLE admin_users (
    id BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
