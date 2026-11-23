-- Default tenant
INSERT INTO tenants (id, name, is_active)
VALUES ('00000000-0000-0000-0000-000000000001', 'default', TRUE);

-- System roles for default tenant
INSERT INTO roles (id, tenant_id, name, description, is_system, is_active)
VALUES
    ('00000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000001',
     'super_admin', 'Full system access, cannot be deleted', TRUE, TRUE),
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000001',
     'admin', 'Administrative access within tenant', TRUE, TRUE),
    ('00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000001',
     'viewer', 'Read-only access', TRUE, TRUE);

-- Role hierarchy: viewer < admin < super_admin
UPDATE roles SET parent_role_id = '00000000-0000-0000-0000-000000000011'
WHERE id = '00000000-0000-0000-0000-000000000012';
UPDATE roles SET parent_role_id = '00000000-0000-0000-0000-000000000010'
WHERE id = '00000000-0000-0000-0000-000000000011';

-- Basic permissions (platform-level)
INSERT INTO permissions (id, resource, action, description, service_name)
VALUES
    ('00000000-0000-0000-0000-000000000100', 'user', 'create', 'Create users', 'users-service'),
    ('00000000-0000-0000-0000-000000000101', 'user', 'read', 'View users', 'users-service'),
    ('00000000-0000-0000-0000-000000000102', 'user', 'update', 'Update users', 'users-service'),
    ('00000000-0000-0000-0000-000000000103', 'user', 'delete', 'Delete users', 'users-service'),
    ('00000000-0000-0000-0000-000000000104', 'role', 'create', 'Create roles', 'users-service'),
    ('00000000-0000-0000-0000-000000000105', 'role', 'read', 'View roles', 'users-service'),
    ('00000000-0000-0000-0000-000000000106', 'role', 'update', 'Update roles', 'users-service'),
    ('00000000-0000-0000-0000-000000000107', 'role', 'delete', 'Delete roles', 'users-service'),
    ('00000000-0000-0000-0000-000000000108', 'role', 'assign', 'Assign roles to users', 'users-service'),
    ('00000000-0000-0000-0000-000000000109', 'permission', 'read', 'View permissions', 'users-service');

-- super_admin gets all permissions with scope 'all'
INSERT INTO role_permissions (role_id, permission_id, scope)
SELECT '00000000-0000-0000-0000-000000000010', id, 'all'
FROM permissions;

-- admin gets user and role management with scope 'all'
INSERT INTO role_permissions (role_id, permission_id, scope)
VALUES
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000100', 'all'),
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000101', 'all'),
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000102', 'all'),
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000105', 'all'),
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000106', 'all'),
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000108', 'all'),
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000109', 'all');

-- viewer gets read-only permissions with scope 'own'
INSERT INTO role_permissions (role_id, permission_id, scope)
VALUES
    ('00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000101', 'own'),
    ('00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000105', 'all'),
    ('00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000109', 'all');
