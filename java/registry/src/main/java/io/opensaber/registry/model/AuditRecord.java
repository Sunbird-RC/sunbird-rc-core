package io.opensaber.registry.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class AuditRecord {

	private JsonNode existingNode;
	private JsonNode latestNode;
	private String action;
	private String recordId;
	private List<Integer> transactionId;
	private String userId;
	private String auditId;
	private String timeStamp;
	private List<AuditInfo> auditInfo;

    @JsonIgnore
	public JsonNode getExistingNode() {
		return existingNode;
	}

	public AuditRecord setExistingNode(JsonNode existingNode) {
		this.existingNode = existingNode;
		return this;
	}
    @JsonIgnore
	public JsonNode getLatestNode() {
		return latestNode;
	}

	public AuditRecord setLatestNode(JsonNode latestNode) {
		this.latestNode = latestNode;
		return this;
	}

	public String getAction() {
		return action;
	}

	public AuditRecord setAction(String action) {
		this.action = action;
		return this;
	}

    public List<Integer> getTransactionId() {
        return transactionId;
    }

    public AuditRecord setTransactionId(List<Integer> transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public AuditRecord setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public List<AuditInfo> getAuditInfo() {
        return auditInfo;
    }

    public AuditRecord setAuditInfo(List<AuditInfo> auditInfo) {
        this.auditInfo = auditInfo;
        return this;
    }

    public String getRecordId() {
        return recordId;
    }

    public AuditRecord setRecordId(String recordId) {
        this.recordId = recordId;
        return this;
    }

    public String getAuditId() {
        return auditId;
    }

    public AuditRecord setAuditId(String auditId) {
        this.auditId = auditId;
        return this;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public AuditRecord setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
        return this;
    }
}
