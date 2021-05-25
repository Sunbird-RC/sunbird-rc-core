package io.opensaber.claim.service;
import io.opensaber.pojos.attestation.AttestationPolicy;

import java.util.List;
import java.util.Map;

public class AttestationPropertiesDTO {
    private Map<String, Object>  entity;
    private List<AttestationPolicy> attestationPolicies;

    public Map<String, Object> getEntity() {
        return entity;
    }

    public void setEntity(Map<String, Object> entity) {
        this.entity = entity;
    }

    public List<AttestationPolicy> getAttestationPolicies() {
        return attestationPolicies;
    }

    public void setAttestationPolicies(List<AttestationPolicy> attestationPolicies) {
        this.attestationPolicies = attestationPolicies;
    }
}
