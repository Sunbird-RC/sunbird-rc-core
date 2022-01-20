package dev.sunbirdrc.registry.entities;

import lombok.Data;

@Data
public class AttestationStep {
    private String osid;
    private String apiURL;
    private String apiMethod;
    private String apiRequestSchema;
}
