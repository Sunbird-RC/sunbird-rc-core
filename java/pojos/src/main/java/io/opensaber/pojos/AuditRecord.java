package io.opensaber.pojos;

import java.util.List;

public class AuditRecord {

	private String action;
	private String recordId;
	private List<Integer> transactionId;
	private String userId;
	private String auditId;
	private String timestamp;
	private List<AuditInfo> auditInfo;

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

    public String getTimestamp() {
        return timestamp;
    }

    public AuditRecord setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }
}
