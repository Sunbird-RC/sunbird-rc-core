package dev.sunbirdrc.registry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostCreateEntityMessage {
    private String userId;
    private String entityType;
    private String osid;
    private String transactionId;
    private CreateEntityStatus status;
    private String message;
    private String webhookUrl;
}

