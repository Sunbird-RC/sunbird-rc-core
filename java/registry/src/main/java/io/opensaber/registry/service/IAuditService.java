package io.opensaber.registry.service;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.tinkerpop.gremlin.structure.Transaction;
import com.fasterxml.jackson.databind.JsonNode;

import io.opensaber.pojos.AuditInfo;
import io.opensaber.pojos.AuditRecord;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.DateUtil;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.sink.shard.Shard;


public interface IAuditService {

    /**
     * This is starting of audit in the application, audit details of read, add, update, delete and search activities
     *
     * 
     */
    void doAudit(AuditRecord auditRecord, JsonNode inputNode, Shard shard);
    
	boolean shouldAudit(String entityType);
	String isAuditAction(String entityType);
	
	List<AuditInfo> createAuditInfo(String auditAction, String entityType);
	List<AuditInfo> createAuditInfoWithJson(String auditAction, JsonNode inputNode, String entityType);
	
	String getAuditProvider();
	
	default  AuditRecord createAuditRecord(String userId, String id, List<Object> transactionId,String entityType){

        AuditRecord auditRecord = new AuditRecord();
        auditRecord.setUserId(userId)
                .setTransactionId(transactionId).setRecordId(id).setEntityType(entityType)
                .setAuditId(UUID.randomUUID().toString()).setTimestamp(String.valueOf(DateUtil.getTimeStampLong()));
        return auditRecord;
	}
	
	default  AuditRecord createAuditRecord(String userId, String id, Transaction tx,String entityType){

        AuditRecord auditRecord = new AuditRecord();
        auditRecord.setUserId(userId)
                .setTransactionId(new LinkedList<>(Arrays.asList(tx.hashCode()))).setRecordId(id).setEntityType(entityType)
                .setAuditId(UUID.randomUUID().toString()).setTimestamp(String.valueOf(DateUtil.getTimeStampLong()));
        return auditRecord;
	}
	
	default  AuditRecord createAuditRecord(String userId, String id, String entityType) {
		//Transaction id is null in case of elastic read service 
		return createAuditRecord(userId, id, new LinkedList<>(Arrays.asList(0)), entityType);
	}
	
	default String getAuditDefinitionName(String entityType, String auditSuffixSeparator, String auditSuffix) {
		if (null != entityType && !(entityType.contains(auditSuffixSeparator + auditSuffix))) {
			entityType = entityType + auditSuffixSeparator + auditSuffix;
		}
		return entityType;
	}
	
	default void auditAdd(AuditRecord auditRecord, Shard shard, JsonNode mergedNode){
		if(shouldAudit(auditRecord.getEntityType())) {
			auditRecord.setAction(Constants.AUDIT_ACTION_ADD);
			JsonNode inputNode = JSONUtil.diffJsonNode(null, mergedNode);
			auditRecord.setAuditInfo(createAuditInfoWithJson(auditRecord.getAction(), inputNode, auditRecord.getEntityType()));
			
			doAudit(auditRecord, mergedNode, shard);
		}
	}
	
	default void auditUpdate(AuditRecord auditRecord, Shard shard, JsonNode mergedNode, JsonNode readNode){
		if(shouldAudit(auditRecord.getEntityType())) {
			auditRecord.setAction(Constants.AUDIT_ACTION_UPDATE);
			JsonNode inputNode = JSONUtil.diffJsonNode(readNode, mergedNode);
			auditRecord.setAuditInfo(createAuditInfoWithJson(auditRecord.getAction(), inputNode, auditRecord.getEntityType()));
			
			doAudit(auditRecord, mergedNode, shard);
		}
	}
	
	default void auditDelete(AuditRecord auditRecord, Shard shard) {
		if(shouldAudit(auditRecord.getEntityType())) {
			auditRecord.setAction(Constants.AUDIT_ACTION_DELETE);
			auditRecord.setAuditInfo(createAuditInfo(auditRecord.getAction(), auditRecord.getEntityType()));
			
			doAudit(auditRecord, null, shard);
		}
	}
	
	default void auditRead(AuditRecord auditRecord, Shard shard) {
		if(shouldAudit(auditRecord.getEntityType())) {
			auditRecord.setAction(Constants.AUDIT_ACTION_READ);
			auditRecord.setAuditInfo(createAuditInfo(auditRecord.getAction(), auditRecord.getEntityType()));
			
			doAudit(auditRecord, null, shard);
		}
	}
	
	// Elastic search audit, no shard info to write to DB
	default void auditElasticSearch(AuditRecord auditRecord, List<String> entityTypes, JsonNode inputNode)  {
		entityTypes.forEach(et -> {
			auditRecord.setEntityType(et).setRecordId("").setTransactionId(new LinkedList<>(Arrays.asList(0)))
			.setAuditId(UUID.randomUUID().toString()).setTimestamp(String.valueOf(DateUtil.getTimeStampLong()));
			
			if(shouldAudit(auditRecord.getEntityType())) {
				auditRecord.setAction(isAuditAction(auditRecord.getEntityType()));
				auditRecord.setAuditInfo(createAuditInfo(auditRecord.getAction(), auditRecord.getEntityType()));
				
				doAudit(auditRecord, inputNode, null);
			}
		});
	}
	
	// Native Search audit 
	default void auditNativeSearch(AuditRecord auditRecord, Shard shard, List<String> entityTypes, JsonNode inputNode)  {
		entityTypes.forEach(et -> {
			auditRecord.setEntityType(et).setRecordId("").setAuditId(UUID.randomUUID().toString()).setTimestamp(String.valueOf(DateUtil.getTimeStampLong()));
			
			if(shouldAudit(auditRecord.getEntityType())) {
				auditRecord.setAction(isAuditAction(auditRecord.getEntityType()));
				auditRecord.setAuditInfo(createAuditInfo(auditRecord.getAction(), auditRecord.getEntityType()));
				
				doAudit(auditRecord, inputNode, shard);
			}
		});
	}

}
