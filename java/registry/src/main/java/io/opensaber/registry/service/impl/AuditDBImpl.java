package io.opensaber.registry.service.impl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import io.opensaber.pojos.AuditRecord;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.sink.shard.Shard;

/**
 * Audit service implementation for audit layer in the application
 */
@Component
public class AuditDBImpl extends AuditServiceImpl {

    private static Logger logger = LoggerFactory.getLogger(AuditDBImpl.class);

    @Autowired
    private AuditDBWriter auditWriter;

    
    /**
     * This is starting of audit in the application, audit details of read, add, update, delete and search activities
     */
    @Override
    public void doAudit(AuditRecord auditRecord, JsonNode inputNode, Shard shard) {
        logger.debug("doAudit started");
        try {
        	JsonNode rootNode = convertAuditRecordToJson(auditRecord, auditRecord.getEntityType());
            auditToDB(rootNode, auditRecord.getEntityType(), shard);

            sendAuditToActor(auditRecord, inputNode, auditRecord.getEntityType());
        } catch (AuditFailedException ae) {
            logger.error("Error in saving audit info: {}", ae);
        } catch (Exception e) {
            logger.error("Generic error in saving audit info : {}", e);
        }
        logger.debug("doAudit ends");
    }
    
    @Async("auditExecutor")
    public void auditToDB(JsonNode rootNode, String entityType, Shard shard) throws IOException, AuditFailedException {
        auditWriter.auditToDB(shard, rootNode, entityType);
        
    }

	@Override
	public String getAuditProvider() {
		
		return Constants.DATABASE;
	}
    
}
