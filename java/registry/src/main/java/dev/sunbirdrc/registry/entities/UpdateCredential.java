package dev.sunbirdrc.registry.entities;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
public class UpdateCredential {
    private String entity;
    private String entityId;
    private String attestationProperty;
    private String attestationPropertyId;
    private String signedData;
    private String signedHash;
    private String userId;
}


