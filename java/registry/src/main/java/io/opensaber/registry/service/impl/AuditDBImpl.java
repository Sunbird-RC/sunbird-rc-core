package io.opensaber.registry.service.impl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import io.opensaber.pojos.AuditRecord;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.model.DBConnectionInfo;
import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.sink.shard.ShardManager;
import io.opensaber.registry.util.Definition;
import io.opensaber.registry.util.DefinitionsManager;

/**
 * Audit service implementation for audit layer in the application
 */
@Component
public class AuditDBImpl extends AuditServiceImpl {

    private static Logger logger = LoggerFactory.getLogger(AuditDBImpl.class);

    @Autowired
    private AuditDBWriter auditWriter;


    @Value("${audit.frame.suffix}")
    private String auditSuffix;

    @Value("${audit.frame.suffixSeparator}")
    private String auditSuffixSeparator;
    
    @Autowired
	private ShardManager shardManager;
    
    
    /**
     * This is starting of audit in the application, audit details of read, add, update, delete and search activities
     */
    @Override
    public void doAudit(AuditRecord auditRecord, JsonNode inputNode, Shard shard) {
        logger.debug("doAudit started");
        try {
        	 //Creating root node with vertex label
      		//by appending the entity name with _Audit
      		String entityType = auditRecord.getEntityType();
      		if( null != entityType && !(entityType.contains(auditSuffixSeparator+auditSuffix))) {
      			entityType = entityType+auditSuffixSeparator+auditSuffix;
      		}
      		
        	JsonNode rootNode = convertAuditRecordToJson(auditRecord, entityType);
            auditToDB(rootNode, entityType, shard);

        } catch (AuditFailedException ae) {
            logger.error("Error in saving audit info: {}", ae);
        } catch (Exception e) {
            logger.error("Generic error in saving audit info : {}", e);
        }
        logger.debug("doAudit ends");
    }
    
    @Async("auditExecutor")
    public void auditToDB(JsonNode rootNode, String entityType, Shard shard) throws IOException, AuditFailedException {
    	
    	if(null == shard) {
    		 shard = shardManager.getDefaultShard();
    	}
    	String entityId = auditWriter.auditToDB(shard, rootNode, entityType);
        sendAuditToESActor(rootNode,entityType,entityId);

    }

	@Override
	public String getAuditProvider() {
		
		return Constants.DATABASE;
	}
    
}
