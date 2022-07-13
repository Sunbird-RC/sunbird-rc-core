package dev.sunbirdrc.registry.entities;

import dev.sunbirdrc.registry.entities.AttestationPolicy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AttestationPolicyTest {
    private AttestationPolicy attestationPolicy;

    @Before
    public void setUp() {
        attestationPolicy = new AttestationPolicy();
    }

    @Test
    public void shouldAbleToGetTheEntityFromPluginUrl() {
        String expectedVal = "Teacher";
        String attestorPlugin = "did:internal:ClaimPluginActor?entity=Teacher";
        attestationPolicy.setAttestorPlugin(attestorPlugin);
        assertEquals(expectedVal, attestationPolicy.getAttestorEntity());
    }

    @Test
    public void shouldReturnCompletionType() {
        attestationPolicy.setOnComplete("attestation:instituteAffiliationCbse");
        Assert.assertEquals(FlowType.ATTESTATION, attestationPolicy.getCompletionType());
        Assert.assertEquals("instituteAffiliationCbse", attestationPolicy.getCompletionValue());
        attestationPolicy.setOnComplete("function:#/functionDefinitions/concat($.pen, $.email, $.instituteName)");
        Assert.assertEquals(FlowType.FUNCTION, attestationPolicy.getCompletionType());
        Assert.assertEquals("#/functionDefinitions/concat($.pen, $.email, $.instituteName)", attestationPolicy.getCompletionValue());
        Assert.assertEquals("concat", attestationPolicy.getCompletionFunctionName());
        attestationPolicy.setOnComplete("function:#/functionDefinitions/userDefinedConcat");
        Assert.assertEquals(FlowType.FUNCTION, attestationPolicy.getCompletionType());
        Assert.assertEquals("#/functionDefinitions/userDefinedConcat", attestationPolicy.getCompletionValue());
        Assert.assertEquals("userDefinedConcat", attestationPolicy.getCompletionFunctionName());
    }
}