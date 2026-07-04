CREATE TYPE item_availability AS ENUM ('AVAILABLE', 'UNAVAILABLE');
CREATE TYPE item_status AS ENUM ('ACTIVE', 'ARCHIVED');

CREATE TABLE items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_unit_id UUID NOT NULL,
    catalog_id UUID NOT NULL,
    price_amount NUMERIC(12, 2),
    availability item_availability NOT NULL DEFAULT 'AVAILABLE',
    configured_quantity INTEGER,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    display_order INTEGER NOT NULL DEFAULT 0,
    status item_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_items_service_unit FOREIGN KEY (service_unit_id) REFERENCES service_units (id),
    CONSTRAINT fk_items_catalog FOREIGN KEY (catalog_id) REFERENCES catalogs (id),
    CONSTRAINT uq_items_service_unit_catalog UNIQUE (service_unit_id, catalog_id),
    CONSTRAINT ck_items_price_amount_non_negative CHECK (price_amount IS NULL OR price_amount >= 0),
    CONSTRAINT ck_items_configured_quantity_non_negative CHECK (
        configured_quantity IS NULL OR configured_quantity >= 0
    ),
    CONSTRAINT ck_items_reserved_quantity_non_negative CHECK (reserved_quantity >= 0),
    CONSTRAINT ck_items_reserved_quantity_within_configured CHECK (
        configured_quantity IS NULL OR reserved_quantity <= configured_quantity
    ),
    CONSTRAINT ck_items_version_positive CHECK (version > 0)
);

CREATE INDEX idx_items_service_unit_id ON items (service_unit_id);
CREATE INDEX idx_items_catalog_id ON items (catalog_id);
CREATE INDEX idx_items_status ON items (status);
CREATE INDEX idx_items_availability ON items (availability);
CREATE INDEX idx_items_service_unit_status_availability
    ON items (service_unit_id, status, availability);
