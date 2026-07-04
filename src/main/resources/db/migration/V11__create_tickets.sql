ALTER TABLE items DROP CONSTRAINT IF EXISTS ck_items_reserved_quantity_within_configured;

CREATE TYPE ticket_status AS ENUM (
    'CREATED',
    'CONFIRMED',
    'CALLED',
    'IN_PROGRESS',
    'COMPLETED',
    'CLOSED',
    'CANCELLED'
);

CREATE TABLE tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_number VARCHAR(40) NOT NULL,
    user_id UUID,
    guest_name VARCHAR(150),
    customer_phone VARCHAR(40),
    guest_access_code_hash VARCHAR(255),
    service_unit_id UUID NOT NULL,
    service_unit_location_id UUID NOT NULL,
    status ticket_status NOT NULL DEFAULT 'CREATED',
    notes TEXT,
    currency CHAR(3) NOT NULL,
    total_amount NUMERIC(12, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at TIMESTAMPTZ,
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_tickets_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_tickets_service_unit FOREIGN KEY (service_unit_id) REFERENCES service_units (id),
    CONSTRAINT fk_tickets_service_unit_location FOREIGN KEY (service_unit_location_id) REFERENCES service_unit_locations (id),
    CONSTRAINT uq_tickets_ticket_number UNIQUE (ticket_number),
    CONSTRAINT ck_tickets_ticket_number_not_blank CHECK (length(trim(ticket_number)) > 0),
    CONSTRAINT ck_tickets_guest_name_not_blank CHECK (guest_name IS NULL OR length(trim(guest_name)) > 0),
    CONSTRAINT ck_tickets_customer_phone_not_blank CHECK (customer_phone IS NULL OR length(trim(customer_phone)) > 0),
    CONSTRAINT ck_tickets_guest_access_code_hash_not_blank CHECK (
        guest_access_code_hash IS NULL OR length(trim(guest_access_code_hash)) > 0
    ),
    CONSTRAINT ck_tickets_currency_format CHECK (currency = upper(currency) AND length(currency) = 3),
    CONSTRAINT ck_tickets_total_amount_non_negative CHECK (total_amount IS NULL OR total_amount >= 0),
    CONSTRAINT ck_tickets_authenticated_or_guest CHECK (
        (user_id IS NOT NULL AND guest_access_code_hash IS NULL)
        OR (user_id IS NULL AND guest_name IS NOT NULL AND guest_access_code_hash IS NOT NULL)
    ),
    CONSTRAINT ck_tickets_closed_at_status CHECK (
        (closed_at IS NULL AND status <> 'CLOSED')
        OR (closed_at IS NOT NULL AND status = 'CLOSED')
    ),
    CONSTRAINT ck_tickets_version_positive CHECK (version > 0)
);

CREATE TABLE ticket_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL,
    item_id UUID NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price_amount NUMERIC(12, 2),
    line_total_amount NUMERIC(12, 2),
    notes TEXT,
    CONSTRAINT fk_ticket_lines_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id),
    CONSTRAINT fk_ticket_lines_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT ck_ticket_lines_quantity_positive CHECK (quantity >= 1),
    CONSTRAINT ck_ticket_lines_unit_price_amount_non_negative CHECK (
        unit_price_amount IS NULL OR unit_price_amount >= 0
    ),
    CONSTRAINT ck_ticket_lines_line_total_amount_non_negative CHECK (
        line_total_amount IS NULL OR line_total_amount >= 0
    )
);

CREATE INDEX idx_tickets_service_unit_id ON tickets (service_unit_id);
CREATE INDEX idx_tickets_service_unit_location_id ON tickets (service_unit_location_id);
CREATE INDEX idx_tickets_user_id ON tickets (user_id);
CREATE INDEX idx_tickets_status ON tickets (status);
CREATE INDEX idx_tickets_service_unit_status ON tickets (service_unit_id, status);
CREATE INDEX idx_tickets_user_status ON tickets (user_id, status);

CREATE INDEX idx_ticket_lines_ticket_id ON ticket_lines (ticket_id);
CREATE INDEX idx_ticket_lines_item_id ON ticket_lines (item_id);
