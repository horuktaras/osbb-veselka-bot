ALTER TABLE requests DROP COLUMN IF EXISTS status_comment;

CREATE TABLE request_comments (
    id         BIGSERIAL PRIMARY KEY,
    request_id BIGINT       NOT NULL REFERENCES requests(id),
    status     VARCHAR(50)  NOT NULL,
    comment    TEXT         NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_request_comments_request_id ON request_comments(request_id);
