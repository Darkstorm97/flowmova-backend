CREATE TYPE company_business_type AS ENUM (
    'RESTAURANT',
    'HAIR_SALON',
    'RETAIL',
    'HEALTHCARE',
    'ADMINISTRATION',
    'SERVICE',
    'OTHER'
);

ALTER TABLE companies
    ADD COLUMN business_type company_business_type NOT NULL DEFAULT 'OTHER';

CREATE INDEX idx_companies_business_type ON companies (business_type);
