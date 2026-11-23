CREATE TABLE service_accounts (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_sub VARCHAR(255) NOT NULL UNIQUE,
    name         VARCHAR(255) NOT NULL,
    tenant_id    UUID NOT NULL REFERENCES tenants(id),
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    version      BIGINT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_service_accounts_tenant_id ON service_accounts(tenant_id);
