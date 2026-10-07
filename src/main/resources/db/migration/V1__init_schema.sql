CREATE TABLE requests (
    id          BIGSERIAL PRIMARY KEY,
    telegram_user_id    BIGINT       NOT NULL,
    telegram_username   VARCHAR(255),
    name                VARCHAR(255) NOT NULL,
    contact             VARCHAR(255) NOT NULL,
    urgent              BOOLEAN      NOT NULL DEFAULT FALSE,
    type                VARCHAR(50)  NOT NULL,
    description         TEXT         NOT NULL,
    media_file_id       VARCHAR(255),
    media_type          VARCHAR(10),
    status              VARCHAR(50)  NOT NULL DEFAULT 'NEW',
    admin_chat_message_id BIGINT,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_requests_telegram_user_id ON requests(telegram_user_id);
CREATE INDEX idx_requests_status ON requests(status);
CREATE INDEX idx_requests_created_at ON requests(created_at);
