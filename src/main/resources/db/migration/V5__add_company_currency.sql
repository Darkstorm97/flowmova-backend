ALTER TABLE companies
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'CAD',
    ADD CONSTRAINT ck_companies_currency_format CHECK (currency ~ '^[A-Z]{3}$');
