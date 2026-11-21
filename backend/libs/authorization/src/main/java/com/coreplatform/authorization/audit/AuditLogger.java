package com.coreplatform.authorization.audit;

public interface AuditLogger {

    void log(AuditEntry entry);
}
