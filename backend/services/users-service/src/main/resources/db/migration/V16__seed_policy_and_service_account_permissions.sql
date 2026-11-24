-- Permissions for service_account management
INSERT INTO permissions (id, resource, action, description, service_name) VALUES
    ('00000000-0000-0000-0000-000000000110', 'service_account', 'create', 'Create service accounts', 'users-service'),
    ('00000000-0000-0000-0000-000000000111', 'service_account', 'read', 'View service accounts', 'users-service'),
    ('00000000-0000-0000-0000-000000000112', 'service_account', 'update', 'Update service accounts', 'users-service'),
    ('00000000-0000-0000-0000-000000000113', 'service_account', 'delete', 'Delete service accounts', 'users-service');

-- Permissions for policy management
INSERT INTO permissions (id, resource, action, description, service_name) VALUES
    ('00000000-0000-0000-0000-000000000120', 'policy', 'create', 'Create policies and policy sets', 'users-service'),
    ('00000000-0000-0000-0000-000000000121', 'policy', 'read', 'View policies and policy sets', 'users-service'),
    ('00000000-0000-0000-0000-000000000122', 'policy', 'update', 'Update policies and policy sets', 'users-service'),
    ('00000000-0000-0000-0000-000000000123', 'policy', 'delete', 'Delete policies and policy sets', 'users-service');

-- Permissions for emergency access
INSERT INTO permissions (id, resource, action, description, service_name) VALUES
    ('00000000-0000-0000-0000-000000000130', 'emergency_access', 'create', 'Grant emergency access', 'users-service'),
    ('00000000-0000-0000-0000-000000000131', 'emergency_access', 'read', 'View emergency access logs', 'users-service'),
    ('00000000-0000-0000-0000-000000000132', 'emergency_access', 'revoke', 'Revoke emergency access', 'users-service');

-- Permission for authorization simulation
INSERT INTO permissions (id, resource, action, description, service_name) VALUES
    ('00000000-0000-0000-0000-000000000140', 'authorization', 'simulate', 'Run authorization simulations', 'users-service');

-- super_admin gets all new permissions with scope 'all'
INSERT INTO role_permissions (role_id, permission_id, scope)
SELECT '00000000-0000-0000-0000-000000000010', id, 'all'
FROM permissions
WHERE id IN (
    '00000000-0000-0000-0000-000000000110',
    '00000000-0000-0000-0000-000000000111',
    '00000000-0000-0000-0000-000000000112',
    '00000000-0000-0000-0000-000000000113',
    '00000000-0000-0000-0000-000000000120',
    '00000000-0000-0000-0000-000000000121',
    '00000000-0000-0000-0000-000000000122',
    '00000000-0000-0000-0000-000000000123',
    '00000000-0000-0000-0000-000000000130',
    '00000000-0000-0000-0000-000000000131',
    '00000000-0000-0000-0000-000000000132',
    '00000000-0000-0000-0000-000000000140'
);

-- admin gets read access to service accounts and policies
INSERT INTO role_permissions (role_id, permission_id, scope) VALUES
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000111', 'all'),
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000121', 'all'),
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000131', 'all'),
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000140', 'all');
