CREATE TABLE tenants (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL UNIQUE,
    is_active  BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE orgs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    name          VARCHAR(255) NOT NULL,
    parent_org_id UUID REFERENCES orgs(id),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_orgs_tenant_id ON orgs(tenant_id);
