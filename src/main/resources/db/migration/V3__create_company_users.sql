CREATE TYPE company_role AS ENUM ('ADMIN', 'EMPLOYEE');

CREATE TYPE company_user_status AS ENUM ('ACTIVE', 'INACTIVE');

CREATE TABLE company_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role company_role NOT NULL DEFAULT 'EMPLOYEE',
    status company_user_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_company_users_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_company_users_company_user UNIQUE (company_id, user_id)
);

CREATE INDEX idx_company_users_company_id ON company_users (company_id);
CREATE INDEX idx_company_users_user_id ON company_users (user_id);
CREATE INDEX idx_company_users_role ON company_users (role);
CREATE INDEX idx_company_users_status ON company_users (status);
