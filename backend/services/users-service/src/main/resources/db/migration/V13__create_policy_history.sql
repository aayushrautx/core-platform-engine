-- Phase 4: Policy change history for auditing

CREATE TABLE policy_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id       UUID NOT NULL,
    policy_set_id   UUID NOT NULL,
    tenant_id       UUID NOT NULL,
    action          VARCHAR(20) NOT NULL CHECK (action IN ('CREATED', 'UPDATED', 'DELETED', 'ACTIVATED', 'DEACTIVATED')),
    old_value       JSONB,
    new_value       JSONB,
    changed_by      UUID,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_policy_history_policy ON policy_history(policy_id);
CREATE INDEX idx_policy_history_tenant ON policy_history(tenant_id, changed_at DESC);
