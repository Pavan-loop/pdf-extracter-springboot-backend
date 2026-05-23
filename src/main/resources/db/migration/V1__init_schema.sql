-- PDFExtractor Initial Schema
-- V1: Full schema for fresh deployments.
-- Existing deployments: Flyway will baseline at V1 (see spring.flyway.baseline-on-migrate).

CREATE TABLE IF NOT EXISTS _user (
    id                         BIGSERIAL PRIMARY KEY,
    name                       VARCHAR(255),
    username                   VARCHAR(255),
    email                      VARCHAR(255) UNIQUE NOT NULL,
    password                   VARCHAR(255),
    phone_number               BIGINT,
    is_account_enabled         BOOLEAN NOT NULL DEFAULT false,
    is_account_non_locked      BOOLEAN NOT NULL DEFAULT true,
    is_seeded_admin            BOOLEAN NOT NULL DEFAULT false,
    plan                       VARCHAR(50) NOT NULL DEFAULT 'FREE',
    pages_uploaded_this_month  INT NOT NULL DEFAULT 0,
    quota_reset_at             TIMESTAMPTZ,
    subscription_expires_at    TIMESTAMPTZ,
    auth_provider              VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    provider_id                VARCHAR(255),
    created_at                 TIMESTAMPTZ NOT NULL,
    modified_at                TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS user_role (
    user_id BIGINT NOT NULL REFERENCES _user(id) ON DELETE CASCADE,
    roles   VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, roles)
);

CREATE TABLE IF NOT EXISTS session (
    id             BIGSERIAL PRIMARY KEY,
    "Name"         VARCHAR(255),
    "DocumentType" VARCHAR(50),
    "Description"  TEXT,
    "ColorCode"    VARCHAR(50),
    user_id        BIGINT REFERENCES _user(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS pdf_results (
    id            BIGSERIAL PRIMARY KEY,
    job_id        VARCHAR(255) UNIQUE NOT NULL,
    user_id       BIGINT NOT NULL,
    filename      VARCHAR(255),
    document_type VARCHAR(255),
    status        VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    extracted_data JSONB,
    error_message TEXT,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ,
    session_id    BIGINT REFERENCES session(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS otp (
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    otp_code   VARCHAR(10)  NOT NULL,
    otp_type   VARCHAR(50)  NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS password_reset_token (
    id         BIGSERIAL PRIMARY KEY,
    token      VARCHAR(255) UNIQUE NOT NULL,
    email      VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used       BOOLEAN NOT NULL DEFAULT false
);

-- Indexes for query performance
CREATE INDEX IF NOT EXISTS idx_user_email          ON _user(email);
CREATE INDEX IF NOT EXISTS idx_user_plan           ON _user(plan);
CREATE INDEX IF NOT EXISTS idx_pdf_results_user_id ON pdf_results(user_id);
CREATE INDEX IF NOT EXISTS idx_pdf_results_status  ON pdf_results(status);
CREATE INDEX IF NOT EXISTS idx_pdf_results_job_id  ON pdf_results(job_id);
CREATE INDEX IF NOT EXISTS idx_otp_email           ON otp(email);
CREATE INDEX IF NOT EXISTS idx_session_user_id     ON session(user_id);
