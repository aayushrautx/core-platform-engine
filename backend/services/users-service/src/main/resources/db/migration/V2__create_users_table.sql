CREATE TABLE users (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_sub VARCHAR(255) NOT NULL UNIQUE,
    username     VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    tenant_id    UUID NOT NULL REFERENCES tenants(id),
    org_id       UUID REFERENCES orgs(id),
    department   VARCHAR(255),
    designation  VARCHAR(255),
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    version      BIGINT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_keycloak_sub ON users(keycloak_sub);
