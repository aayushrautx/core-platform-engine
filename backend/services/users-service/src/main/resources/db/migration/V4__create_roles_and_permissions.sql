CREATE TABLE permissions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource     VARCHAR(255) NOT NULL,
    action       VARCHAR(255) NOT NULL,
    description  TEXT,
    service_name VARCHAR(255),
    UNIQUE(resource, action)
);

CREATE TABLE roles (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID NOT NULL REFERENCES tenants(id),
    name           VARCHAR(255) NOT NULL,
    description    TEXT,
    parent_role_id UUID REFERENCES roles(id),
    is_system      BOOLEAN NOT NULL DEFAULT FALSE,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    version        BIGINT NOT NULL DEFAULT 0,
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(tenant_id, name)
);

CREATE INDEX idx_roles_tenant_id ON roles(tenant_id);
