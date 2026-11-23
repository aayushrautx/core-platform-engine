CREATE TABLE authorization_change_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID,
    entity_type VARCHAR(100) NOT NULL,
    entity_id   UUID NOT NULL,
    action      VARCHAR(50) NOT NULL,
    old_value   JSONB,
    new_value   JSONB,
    timestamp   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_change_log_entity
    ON authorization_change_log(entity_type, entity_id);

CREATE INDEX idx_change_log_actor
    ON authorization_change_log(actor_id);
