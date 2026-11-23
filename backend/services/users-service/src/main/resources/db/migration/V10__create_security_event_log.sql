CREATE TABLE security_event_log (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    actor_id   UUID,
    target_id  UUID,
    tenant_id  UUID,
    details    JSONB,
    request_ip VARCHAR(45),
    timestamp  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_security_event_type
    ON security_event_log(event_type);

CREATE INDEX idx_security_event_tenant
    ON security_event_log(tenant_id, timestamp);
