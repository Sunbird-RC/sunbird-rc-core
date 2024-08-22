package dev.sunbirdrc.pojos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuditRecord {
	private String action;
	private String recordId;
	private List<Object> transactionId;
	private String userId;
	private String auditId;
	private String timestamp;
    private List<AuditInfo> auditInfo;
	private String entityType;
}
