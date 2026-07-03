CREATE TYPE service_unit_location_type AS ENUM ('DEFAULT', 'CUSTOM');
CREATE TYPE service_unit_location_status AS ENUM ('ACTIVE', 'ARCHIVED');

CREATE TABLE service_unit_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_unit_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    type service_unit_location_type NOT NULL DEFAULT 'CUSTOM',
    is_default BOOLEAN NOT NULL DEFAULT false,
    public_access_slug VARCHAR(120) NOT NULL,
    status service_unit_location_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID,
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_service_unit_locations_service_unit FOREIGN KEY (service_unit_id) REFERENCES service_units (id),
    CONSTRAINT fk_service_unit_locations_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_service_unit_locations_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_service_unit_locations_public_access_slug UNIQUE (public_access_slug),
    CONSTRAINT ck_service_unit_locations_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_service_unit_locations_public_access_slug_not_blank CHECK (length(trim(public_access_slug)) > 0),
    CONSTRAINT ck_service_unit_locations_default_type CHECK (
        (is_default = true AND type = 'DEFAULT')
        OR (is_default = false AND type = 'CUSTOM')
    ),
    CONSTRAINT ck_service_unit_locations_version_positive CHECK (version > 0)
);

CREATE UNIQUE INDEX uq_service_unit_locations_one_active_default
    ON service_unit_locations (service_unit_id)
    WHERE is_default = true AND status = 'ACTIVE';

CREATE INDEX idx_service_unit_locations_service_unit_id ON service_unit_locations (service_unit_id);
CREATE INDEX idx_service_unit_locations_status ON service_unit_locations (status);
CREATE INDEX idx_service_unit_locations_is_default ON service_unit_locations (is_default);
CREATE INDEX idx_service_unit_locations_public_access_slug ON service_unit_locations (public_access_slug);
CREATE INDEX idx_service_unit_locations_service_unit_status
    ON service_unit_locations (service_unit_id, status);
