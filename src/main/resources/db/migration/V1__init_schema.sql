CREATE TABLE telegram_users (
    id BIGSERIAL PRIMARY KEY,
    telegram_user_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_telegram_users_telegram_user_id ON telegram_users(telegram_user_id);

CREATE TABLE chat_configs (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL UNIQUE,
    verification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    verification_timeout_seconds INTEGER NOT NULL DEFAULT 600,
    anti_link_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    anti_spam_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    max_messages_per_window INTEGER NOT NULL DEFAULT 10,
    rate_limit_window_seconds INTEGER NOT NULL DEFAULT 60,
    warning_limit INTEGER NOT NULL DEFAULT 3,
    warning_punishment VARCHAR(50) NOT NULL DEFAULT 'MUTE',
    warning_punishment_duration_seconds INTEGER NOT NULL DEFAULT 86400,
    allowed_domains TEXT,
    blacklisted_keywords TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_chat_configs_chat_id ON chat_configs(chat_id);

CREATE TABLE chat_members (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    telegram_user_id BIGINT NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    verification_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    verified_at TIMESTAMP WITH TIME ZONE,
    muted_until TIMESTAMP WITH TIME ZONE,
    banned BOOLEAN NOT NULL DEFAULT FALSE,
    warning_count INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_chat_members_chat_user UNIQUE (chat_id, telegram_user_id)
);
CREATE INDEX idx_chat_members_chat_id ON chat_members(chat_id);
CREATE INDEX idx_chat_members_telegram_user_id ON chat_members(telegram_user_id);
CREATE INDEX idx_chat_members_chat_user ON chat_members(chat_id, telegram_user_id);

CREATE TABLE verifications (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    telegram_user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    verification_message_id BIGINT
);
CREATE INDEX idx_verifications_chat_user ON verifications(chat_id, telegram_user_id);
CREATE INDEX idx_verifications_expires_at ON verifications(expires_at);
CREATE INDEX idx_verifications_status ON verifications(status);

CREATE TABLE moderation_logs (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    admin_telegram_user_id BIGINT,
    target_telegram_user_id BIGINT NOT NULL,
    action VARCHAR(100) NOT NULL,
    reason TEXT,
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_moderation_logs_chat_id ON moderation_logs(chat_id);
CREATE INDEX idx_moderation_logs_target_user ON moderation_logs(target_telegram_user_id);
CREATE INDEX idx_moderation_logs_created_at ON moderation_logs(created_at);

CREATE TABLE processed_updates (
    id BIGSERIAL PRIMARY KEY,
    update_id INTEGER NOT NULL UNIQUE,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_processed_updates_update_id ON processed_updates(update_id);
CREATE INDEX idx_processed_updates_processed_at ON processed_updates(processed_at);
