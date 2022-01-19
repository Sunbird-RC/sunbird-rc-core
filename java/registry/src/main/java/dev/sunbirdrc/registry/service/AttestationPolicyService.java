package dev.sunbirdrc.registry.service;

import dev.sunbirdrc.pojos.attestation.exception.PolicyNotFoundException;
import dev.sunbirdrc.registry.entities.AttestationPolicy;
import dev.sunbirdrc.registry.repositories.AttestationPolicyRepository;
import dev.sunbirdrc.registry.util.DefinitionsManager;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttestationPolicyService {
    @Autowired
    private AttestationPolicyRepository attestationPolicyRepository;

    @Autowired
    private DefinitionsManager definitionsManager;

    //TODO: add cacheable
    public List<AttestationPolicy> getAttestationPolicies(String entityName) {
        List<AttestationPolicy> dbAttestationPolicies = attestationPolicyRepository.findAllByEntity(entityName);
        List<AttestationPolicy> schemaAttestationPolicies = definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getAttestationPolicies();
        return ListUtils.union(dbAttestationPolicies, schemaAttestationPolicies);
    }

    public boolean isPolicyNameAlreadyExistsInSchema(String entityName, String policyName) {
        List<AttestationPolicy> schemaAttestationPolicies = definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getAttestationPolicies();
        return schemaAttestationPolicies.stream().anyMatch(policy -> policy.getName().equals(policyName));
    }

    //TODO: add cacheable
    public AttestationPolicy getAttestationPolicy(String entityName, String policyName) {
        List<AttestationPolicy> attestationPolicies = getAttestationPolicies(entityName);
        return attestationPolicies.stream()
                .filter(policy -> policy.getName().equals(policyName))
                .findFirst()
                .orElseThrow(() -> new PolicyNotFoundException("Policy " + policyName + " is not found"));
    }
}
