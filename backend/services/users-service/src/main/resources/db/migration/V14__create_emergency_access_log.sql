-- Phase 6: Emergency access audit log

CREATE TABLE emergency_access_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    granted_to      UUID NOT NULL,
    granted_by      UUID NOT NULL,
    reason          TEXT NOT NULL,
    permissions     JSONB NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,
    revoked_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_emergency_access_tenant ON emergency_access_log(tenant_id, created_at DESC);
CREATE INDEX idx_emergency_access_active ON emergency_access_log(expires_at) WHERE revoked_at IS NULL;
