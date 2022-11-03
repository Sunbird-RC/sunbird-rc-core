package dev.sunbirdrc.registry.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateEntityMessage {
    private String userId;
    private JsonNode inputJson;
    private boolean skipSignature;
    private String webhookUrl;
    private String emailId;
}
