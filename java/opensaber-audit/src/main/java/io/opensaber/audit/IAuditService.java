package io.opensaber.audit;

import io.opensaber.pojos.AuditRecord;
import java.io.IOException;

public interface IAuditService {

    /**
     * This is starting of audit in the application, audit details of read, add, update, delete and search activities
     *
     * @param auditRecord - input audit details
     * @throws IOException
     */
    public void audit(AuditRecord auditRecord) throws IOException;
}
