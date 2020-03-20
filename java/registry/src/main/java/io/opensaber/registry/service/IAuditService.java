package io.opensaber.registry.service;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.opensaber.pojos.AuditInfo;
import io.opensaber.pojos.AuditRecord;
import io.opensaber.registry.sink.shard.Shard;
import org.apache.tinkerpop.gremlin.structure.Transaction;

public interface IAuditService {

    /**
     * This is starting of audit in the application, audit details of read, add, update, delete and search activities
     *
     * 
     */
    void doAudit(AuditRecord auditRecord, JsonNode mergedNode, String entityType, String entityRootId, Shard shard);
    void auditToFile(AuditRecord auditRecord) throws JsonProcessingException;
	boolean shouldAudit(String entityType);
	AuditRecord createAuditRecord(String userId, String auditAction, String id, List<Object> transactionId)
			throws JsonProcessingException;
	List<AuditInfo> createAuditInfo(String operation, String auditAction, JsonNode readNode, JsonNode mergedNode,
			List<String> entityType) throws JsonProcessingException;


	default String getAuditDefinitionName(String entityType, String auditSuffixSeparator, String auditSuffix) {
		if (null != entityType && !(entityType.contains(auditSuffixSeparator + auditSuffix))) {
			entityType = entityType + auditSuffixSeparator + auditSuffix;
		}
		return entityType;
	}

	default void doAudit(Shard shard, String userId, String entityId,
						 String entityType, Transaction tx, String operation,
						 List<String> entityTypes) throws JsonProcessingException {
		if(shouldAudit(entityType)) {
			List<Object> transactionId = new LinkedList<>(Arrays.asList(tx.hashCode()));

			AuditRecord auditRecord = createAuditRecord(userId, operation, entityId, transactionId);
			auditRecord.setAuditInfo(createAuditInfo(operation, operation, null, null, entityTypes));
			entityTypes.forEach(et -> {
				doAudit(auditRecord, null, et, entityId, shard);
			});
		}
	}

	default void doAudit(Shard shard, String userId,
						 String operation,
						 List<String> entityTypes,
						 List<Object> transactionIds) throws JsonProcessingException {

		entityTypes.forEach(et -> {

			AuditRecord auditRecord = createAuditRecord(userId, operation, null, transactionId);
			auditRecord.setAuditInfo(createAuditInfo(operation, operation, null, null, entityTypes));
				doAudit(auditRecord, null, et, null, shard);
			});
		}
	}

}
