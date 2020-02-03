package io.opensaber.registry.service;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.opensaber.pojos.AuditInfo;
import io.opensaber.pojos.AuditRecord;
import io.opensaber.registry.sink.shard.Shard;

public interface IAuditService {

    /**
     * This is starting of audit in the application, audit details of read, add, update, delete and search activities
     *
     * 
     */
    public void doAudit(AuditRecord auditRecord, JsonNode mergedNode, List<String> entityTypes, String entityRootId, Shard shard);
    public void auditToFile(AuditRecord auditRecord) throws JsonProcessingException;
	AuditRecord createAuditRecord(String userId, String auditAction, String id, List<Integer> transactionId)
			throws JsonProcessingException;
	List<AuditInfo> createAuditInfo(String operation, String auditAction, JsonNode readNode, JsonNode mergedNode,
			List<String> entityTypes) throws JsonProcessingException;
}
