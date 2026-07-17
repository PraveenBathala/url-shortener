CREATE TABLE redirect_analytics (
    short_code         VARCHAR(16)  NOT NULL,
    redirect_count     BIGINT       NOT NULL DEFAULT 0,
    not_found_count    BIGINT       NOT NULL DEFAULT 0,
    expired_count      BIGINT       NOT NULL DEFAULT 0,
    disabled_count     BIGINT       NOT NULL DEFAULT 0,
    last_event_at      TIMESTAMPTZ  NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_redirect_analytics PRIMARY KEY (short_code)
);
