package io.opensaber.registry.service;

import io.opensaber.registry.model.AuditRecord;
import java.io.IOException;

public interface IAuditService {

    /**
     * This is starting of audit in the application, audit details of read, add, update, delete and search activities
     *
     * @param auditRecord - input audit details
     * @throws IOException
     */
    void audit(AuditRecord auditRecord) throws IOException;
}
