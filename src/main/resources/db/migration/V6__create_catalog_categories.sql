CREATE TYPE catalog_category_status AS ENUM ('ACTIVE', 'ARCHIVED');

CREATE TABLE catalog_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    status catalog_category_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID,
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_catalog_categories_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_catalog_categories_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_catalog_categories_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_catalog_categories_company_name UNIQUE (company_id, name),
    CONSTRAINT ck_catalog_categories_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_catalog_categories_display_order_non_negative CHECK (display_order >= 0),
    CONSTRAINT ck_catalog_categories_version_positive CHECK (version > 0)
);

CREATE INDEX idx_catalog_categories_company_id ON catalog_categories (company_id);
CREATE INDEX idx_catalog_categories_company_status ON catalog_categories (company_id, status);
CREATE INDEX idx_catalog_categories_company_display_order_name
    ON catalog_categories (company_id, display_order, name);
