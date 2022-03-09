package dev.sunbirdrc.registry.entities;

import dev.sunbirdrc.registry.entities.AttestationPolicy;
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
}