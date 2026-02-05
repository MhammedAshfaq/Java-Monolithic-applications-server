-- 20260205120000__create_users_table.sql
-- Creates the users table

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'USER',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    last_login_at   TIMESTAMP
);

-- Index for email lookups (login)
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Index for status queries
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- Composite index for active user lookups
CREATE INDEX IF NOT EXISTS idx_users_email_status ON users(email, status);

COMMENT ON TABLE users IS 'User accounts for authentication';
COMMENT ON COLUMN users.role IS 'USER, ADMIN, MODERATOR';
COMMENT ON COLUMN users.status IS 'ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION';
