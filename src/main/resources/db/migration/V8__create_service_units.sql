CREATE TYPE service_unit_type AS ENUM ('TICKET_QUEUE');
CREATE TYPE service_unit_status AS ENUM ('CLOSED', 'OPEN', 'ARCHIVED');

CREATE TABLE service_units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    location VARCHAR(255),
    type service_unit_type NOT NULL DEFAULT 'TICKET_QUEUE',
    status service_unit_status NOT NULL DEFAULT 'CLOSED',
    settings JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID,
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_service_units_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_service_units_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_service_units_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT ck_service_units_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_service_units_settings_object CHECK (jsonb_typeof(settings) = 'object'),
    CONSTRAINT ck_service_units_version_positive CHECK (version > 0)
);

CREATE INDEX idx_service_units_company_id ON service_units (company_id);
CREATE INDEX idx_service_units_status ON service_units (status);
CREATE INDEX idx_service_units_type ON service_units (type);
CREATE INDEX idx_service_units_company_status ON service_units (company_id, status);
CREATE INDEX idx_service_units_company_type ON service_units (company_id, type);
