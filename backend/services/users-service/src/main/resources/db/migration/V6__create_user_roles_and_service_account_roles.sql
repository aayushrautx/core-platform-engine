CREATE TABLE user_roles (
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id     UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT now(),
    assigned_by UUID REFERENCES users(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

CREATE TABLE service_account_roles (
    service_account_id UUID NOT NULL REFERENCES service_accounts(id) ON DELETE CASCADE,
    role_id            UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (service_account_id, role_id)
);

CREATE INDEX idx_service_account_roles_role_id ON service_account_roles(role_id);
