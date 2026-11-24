-- Phase 4: Policy Sets and Policies

CREATE TABLE policy_sets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    target_resource VARCHAR(100) NOT NULL,
    target_action   VARCHAR(100) NOT NULL,
    combining_strategy VARCHAR(50) NOT NULL DEFAULT 'DENY_OVERRIDES',
    is_active       BOOLEAN NOT NULL DEFAULT true,
    version         BIGINT NOT NULL DEFAULT 1,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_policy_set_tenant_resource_action UNIQUE (tenant_id, name)
);

CREATE INDEX idx_policy_sets_tenant_resource ON policy_sets(tenant_id, target_resource, target_action);
CREATE INDEX idx_policy_sets_active ON policy_sets(is_active) WHERE is_active = true;

CREATE TABLE policies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_set_id   UUID NOT NULL REFERENCES policy_sets(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    effect          VARCHAR(10) NOT NULL CHECK (effect IN ('PERMIT', 'DENY')),
    override_rbac   BOOLEAN NOT NULL DEFAULT false,
    conditions_json JSONB,
    obligations_json JSONB,
    priority        INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    version         BIGINT NOT NULL DEFAULT 1,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_conditions_size CHECK (octet_length(conditions_json::text) <= 16384),
    CONSTRAINT chk_obligations_size CHECK (octet_length(obligations_json::text) <= 16384)
);

CREATE INDEX idx_policies_policy_set ON policies(policy_set_id);
CREATE INDEX idx_policies_active ON policies(is_active) WHERE is_active = true;
CREATE INDEX idx_policies_priority ON policies(policy_set_id, priority);
