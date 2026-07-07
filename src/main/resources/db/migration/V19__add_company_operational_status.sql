CREATE TYPE company_operational_status AS ENUM ('OPEN', 'CLOSED');

ALTER TABLE companies
    ADD COLUMN operational_status company_operational_status NOT NULL DEFAULT 'OPEN';

CREATE INDEX idx_companies_operational_status ON companies (operational_status);
CREATE INDEX idx_companies_status_operational_status ON companies (status, operational_status);
