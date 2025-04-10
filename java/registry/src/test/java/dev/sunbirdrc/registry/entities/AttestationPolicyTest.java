package dev.sunbirdrc.registry.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttestationPolicyTest {
    private AttestationPolicy attestationPolicy;

    @BeforeEach
    void setUp() {
        attestationPolicy = new AttestationPolicy();
    }

    @Test
    void shouldAbleToGetTheEntityFromPluginUrl() {
        String expectedVal = "Teacher";
        String attestorPlugin = "did:internal:ClaimPluginActor?entity=Teacher";
        attestationPolicy.setAttestorPlugin(attestorPlugin);
        assertEquals(expectedVal, attestationPolicy.getAttestorEntity());
    }

    @Test
    void shouldReturnCompletionType() {
        attestationPolicy.setOnComplete("attestation:instituteAffiliationCbse");
        assertEquals(FlowType.ATTESTATION, attestationPolicy.getCompletionType());
        assertEquals("instituteAffiliationCbse", attestationPolicy.getCompletionValue());
        attestationPolicy.setOnComplete("function:#/functionDefinitions/concat($.pen, $.email, $.instituteName)");
        assertEquals(FlowType.FUNCTION, attestationPolicy.getCompletionType());
        assertEquals("#/functionDefinitions/concat($.pen, $.email, $.instituteName)", attestationPolicy.getCompletionValue());
        assertEquals("concat", attestationPolicy.getCompletionFunctionName());
        attestationPolicy.setOnComplete("function:#/functionDefinitions/userDefinedConcat");
        assertEquals(FlowType.FUNCTION, attestationPolicy.getCompletionType());
        assertEquals("#/functionDefinitions/userDefinedConcat", attestationPolicy.getCompletionValue());
        assertEquals("userDefinedConcat", attestationPolicy.getCompletionFunctionName());
    }
}