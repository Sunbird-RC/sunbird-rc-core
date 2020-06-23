package io.opensaber.registry.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import io.opensaber.pojos.AuditRecord;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.util.AuditFileWriter;

/**
 * Audit service implementation for audit layer in the application
 */
@Component
public class AuditFileImpl extends AuditServiceImpl {

    private static Logger logger = LoggerFactory.getLogger(AuditFileImpl.class);
   

    /**
     * This is starting of audit in the application, audit details of read, add, update, delete and search activities
     */
    @Override
    public void doAudit(AuditRecord auditRecord, JsonNode inputNode, Shard shard) {
        logger.debug("doAudit started");
        try {
            // If the audit is stored as file, fetchAudit from audit entity will not come to this point.
        	AuditFileWriter auditWriter = new AuditFileWriter();
            auditWriter.auditToFile(auditRecord);

           // sendAuditToActor(auditRecord, inputNode, auditRecord.getEntityType());
        } catch (Exception e) {
            logger.error("Generic error in saving audit info : {}", e);
        }
        logger.debug("doAudit ends");
	}  
    
    @Override
	public String getAuditProvider() {
		
		return Constants.FILE;
	}
}
