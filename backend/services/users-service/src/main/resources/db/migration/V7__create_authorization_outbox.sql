CREATE TABLE authorization_outbox (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type  VARCHAR(100) NOT NULL,
    tenant_id   UUID,
    entity_type VARCHAR(100) NOT NULL,
    entity_id   UUID NOT NULL,
    version     BIGINT NOT NULL,
    payload     JSONB,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    published   BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_authorization_outbox_unpublished
    ON authorization_outbox(created_at) WHERE published = FALSE;
