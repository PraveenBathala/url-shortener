CREATE TABLE short_urls (
    short_code      VARCHAR(16)  NOT NULL,
    destination_url TEXT         NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    expires_at      TIMESTAMPTZ  NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_short_urls PRIMARY KEY (short_code),
    CONSTRAINT chk_short_urls_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT chk_short_urls_destination_not_blank CHECK (char_length(btrim(destination_url)) > 0)
);

-- Supports cleanup/expiry jobs and operational queries for expired active links.
CREATE INDEX idx_short_urls_expires_at_active
    ON short_urls (expires_at)
    WHERE status = 'ACTIVE' AND expires_at IS NOT NULL;
