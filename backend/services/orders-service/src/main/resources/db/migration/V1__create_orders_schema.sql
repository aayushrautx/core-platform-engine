CREATE SCHEMA IF NOT EXISTS orders;

CREATE TABLE orders.orders (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    owner_id        UUID NOT NULL,
    owner_keycloak_sub VARCHAR(255) NOT NULL,
    department      VARCHAR(255) NOT NULL,
    description     VARCHAR(500) NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_tenant_id ON orders.orders (tenant_id);
CREATE INDEX idx_orders_owner_sub ON orders.orders (tenant_id, owner_keycloak_sub);
CREATE INDEX idx_orders_department ON orders.orders (tenant_id, department);
