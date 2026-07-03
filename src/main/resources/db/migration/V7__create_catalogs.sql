CREATE TYPE catalog_status AS ENUM ('ACTIVE', 'ARCHIVED');

CREATE TABLE catalogs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    catalog_category_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    image_url VARCHAR(2000),
    price_amount NUMERIC(12, 2),
    status catalog_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID,
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_catalogs_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_catalogs_catalog_category FOREIGN KEY (catalog_category_id) REFERENCES catalog_categories (id),
    CONSTRAINT fk_catalogs_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_catalogs_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT ck_catalogs_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_catalogs_price_amount_non_negative CHECK (price_amount IS NULL OR price_amount >= 0),
    CONSTRAINT ck_catalogs_version_positive CHECK (version > 0)
);

CREATE INDEX idx_catalogs_company_id ON catalogs (company_id);
CREATE INDEX idx_catalogs_catalog_category_id ON catalogs (catalog_category_id);
CREATE INDEX idx_catalogs_status ON catalogs (status);
CREATE INDEX idx_catalogs_company_status ON catalogs (company_id, status);
CREATE INDEX idx_catalogs_company_category_status ON catalogs (company_id, catalog_category_id, status);
