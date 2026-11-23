CREATE TABLE authorization_audit_log (
    id             UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id        UUID,
    tenant_id      UUID,
    resource       VARCHAR(255) NOT NULL,
    resource_id    VARCHAR(255),
    action         VARCHAR(255) NOT NULL,
    decision       VARCHAR(10) NOT NULL,
    matched_policy VARCHAR(255),
    rbac_result    BOOLEAN,
    reason         TEXT,
    request_ip     VARCHAR(45),
    timestamp      TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (id, timestamp)
) PARTITION BY RANGE (timestamp);

CREATE INDEX idx_audit_log_tenant_timestamp
    ON authorization_audit_log(tenant_id, timestamp);

CREATE INDEX idx_audit_log_user_id
    ON authorization_audit_log(user_id);

-- Initial partition for current month
CREATE TABLE authorization_audit_log_default
    PARTITION OF authorization_audit_log DEFAULT;
