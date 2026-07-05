DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'ticket_creation_guard_mode') THEN
        CREATE TYPE ticket_creation_guard_mode AS ENUM (
            'NONE',
            'AUTHENTICATED_ONLY_ONE_OPEN_TICKET',
            'AUTHENTICATED_OR_GUEST_RECENT_ONE_OPEN_TICKET'
        );
    END IF;
END
$$;

ALTER TABLE service_units
    ADD COLUMN IF NOT EXISTS ticket_creation_guard_mode ticket_creation_guard_mode NOT NULL DEFAULT 'NONE';

UPDATE service_units
SET ticket_creation_guard_mode = 'AUTHENTICATED_ONLY_ONE_OPEN_TICKET'
WHERE settings ->> 'oneActiveTicketPerUser' = 'true';
