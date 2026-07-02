CREATE TYPE user_status AS ENUM ('ACTIVE', 'DISABLED');

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(50),
    profile_picture VARCHAR(500),
    preferred_language VARCHAR(20),
    status user_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_not_blank CHECK (length(trim(email)) > 0),
    CONSTRAINT ck_users_first_name_not_blank CHECK (length(trim(first_name)) > 0),
    CONSTRAINT ck_users_last_name_not_blank CHECK (length(trim(last_name)) > 0),
    CONSTRAINT ck_users_version_positive CHECK (version > 0)
);

CREATE INDEX idx_users_status ON users (status);
