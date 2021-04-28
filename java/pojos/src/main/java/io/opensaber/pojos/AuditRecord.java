package io.opensaber.pojos;

import java.util.List;

public class AuditRecord {
	private String action;
	private String recordId;
	private List<Object> transactionId;
	private String userId;
	private String auditId;
	private String timestamp;
	private List<AuditInfo> auditInfo;
	private String entityType;

	public String getEntityType() {
		return entityType;
	}

	public AuditRecord setEntityType(String entityType) {
		this.entityType = entityType;
		return this;
	}

	public String getAction() {
		return action;
	}

	public AuditRecord setAction(String action) {
		this.action = action;
		return this;
	}

    public List<Object> getTransactionId() {
        return transactionId;
    }

    public AuditRecord setTransactionId(List<Object> transactionId) {
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

    public String getTimestamp() {
        return timestamp;
    }

    public AuditRecord setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }
}
