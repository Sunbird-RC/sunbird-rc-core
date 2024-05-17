package dev.sunbirdrc.registry.service.impl;

import dev.sunbirdrc.registry.exception.AuditFailedException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import dev.sunbirdrc.pojos.AuditRecord;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.sink.shard.Shard;
import dev.sunbirdrc.registry.util.AuditFileWriter;

/**
 * Audit service implementation for audit layer in the application
 */
@Component
@ConditionalOnExpression("${audit.enabled} and 'file'.equalsIgnoreCase('${audit.frame.store}')")
public class AuditFileImpl extends AuditServiceImpl {

    private static Logger logger = LoggerFactory.getLogger(AuditFileImpl.class);
   

    /**
     * This is starting of audit in the application, audit details of read, add, update, delete and search activities
     */
    @Override
    public void doAudit(AuditRecord auditRecord, JsonNode inputNode, Shard shard) throws AuditFailedException {
        logger.debug("doAudit started");
        try {
            // If the audit is stored as file, fetchAudit from audit entity will not come to this point.
        	AuditFileWriter auditWriter = new AuditFileWriter();
            JsonNode rootNode = convertAuditRecordToJson(auditRecord, auditRecord.getEntityType());
            signAudit(auditRecord.getEntityType(), rootNode);
            auditWriter.auditToFile(rootNode);

           // sendAuditToActor(auditRecord, inputNode, auditRecord.getEntityType());
        } catch (Exception e) {
            logger.error("Generic error in saving audit info : {}", ExceptionUtils.getStackTrace(e));
            throw new AuditFailedException("Audit failed: " + e.getMessage());
        }
        logger.debug("doAudit ends");
	}  
    
    @Override
	public String getAuditProvider() {
		return Constants.FILE;
	}
}
