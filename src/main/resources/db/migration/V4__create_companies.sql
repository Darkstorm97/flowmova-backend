CREATE TYPE company_status AS ENUM ('ACTIVE', 'DISABLED');

CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status company_status NOT NULL DEFAULT 'DISABLED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID,
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_companies_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_companies_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT ck_companies_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_companies_version_positive CHECK (version > 0)
);

CREATE INDEX idx_companies_status ON companies (status);
CREATE INDEX idx_companies_name ON companies (name);
CREATE INDEX idx_companies_status_name ON companies (status, name);
CREATE INDEX idx_companies_created_by ON companies (created_by);
