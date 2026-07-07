DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'service_unit_creation_entry_mode') THEN
        CREATE TYPE service_unit_creation_entry_mode AS ENUM ('PUBLIC_AND_QR', 'QR_ONLY');
    END IF;
END
$$;

ALTER TABLE service_units
    ADD COLUMN IF NOT EXISTS creation_entry_mode service_unit_creation_entry_mode NOT NULL DEFAULT 'PUBLIC_AND_QR';

CREATE INDEX IF NOT EXISTS idx_service_units_creation_entry_mode
    ON service_units (creation_entry_mode);

CREATE INDEX IF NOT EXISTS idx_service_units_company_creation_entry_mode
    ON service_units (company_id, creation_entry_mode);
