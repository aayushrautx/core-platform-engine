-- Phase 6.3: Audit log partitioning for authorization_audit_log
-- Monthly partitions with 12-month retention policy
-- authorization_change_log retained indefinitely (compliance requirement)

-- Step 1: Create partitioned audit log table
CREATE TABLE IF NOT EXISTS authorization_audit_log_partitioned (
    id UUID NOT NULL,
    user_id UUID,
    principal_type VARCHAR(50),
    tenant_id UUID,
    resource VARCHAR(255),
    resource_id VARCHAR(255),
    action VARCHAR(100),
    allowed BOOLEAN NOT NULL,
    rbac_result BOOLEAN,
    reason TEXT,
    request_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Step 2: Create initial monthly partitions (current month + next 3 months)
-- Additional partitions should be created by a scheduled job
DO $$
DECLARE
    start_date DATE;
    end_date DATE;
    partition_name TEXT;
    i INT;
BEGIN
    FOR i IN 0..3 LOOP
        start_date := date_trunc('month', CURRENT_DATE) + (i || ' months')::interval;
        end_date := start_date + '1 month'::interval;
        partition_name := 'authorization_audit_log_p' || to_char(start_date, 'YYYY_MM');

        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I PARTITION OF authorization_audit_log_partitioned
             FOR VALUES FROM (%L) TO (%L)',
            partition_name, start_date, end_date
        );
    END LOOP;
END $$;

-- Step 3: Create default partition for out-of-range data
CREATE TABLE IF NOT EXISTS authorization_audit_log_default
    PARTITION OF authorization_audit_log_partitioned DEFAULT;

-- Step 4: Create indexes on partitioned table
CREATE INDEX IF NOT EXISTS idx_audit_log_part_tenant_created
    ON authorization_audit_log_partitioned (tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_part_user_created
    ON authorization_audit_log_partitioned (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_part_resource
    ON authorization_audit_log_partitioned (resource, action, created_at DESC);

-- Note: Migration of existing data from authorization_audit_log to
-- authorization_audit_log_partitioned should be done in a maintenance window.
-- After migration, rename tables:
--   ALTER TABLE authorization_audit_log RENAME TO authorization_audit_log_legacy;
--   ALTER TABLE authorization_audit_log_partitioned RENAME TO authorization_audit_log;
--
-- Retention policy: drop partitions older than 12 months via scheduled job:
--   DROP TABLE IF EXISTS authorization_audit_log_pYYYY_MM;
--
-- authorization_change_log is NOT partitioned (compliance: retained indefinitely)
