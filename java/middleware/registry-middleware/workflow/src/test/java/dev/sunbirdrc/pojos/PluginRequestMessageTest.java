package dev.sunbirdrc.pojos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginRequestMessageTest {

    PluginRequestMessage pluginRequestMessage;
    final String pluginName = "ClaimPluginActor";

    @BeforeEach
    void setUp() {
        pluginRequestMessage = new PluginRequestMessage();
    }

    @Test
    void shouldAbleToGetActorNameIfParamsIsNotPresent() {
        String attestationPlugin = "did:internal:ClaimPluginActor?entity=Teacher";
        pluginRequestMessage.setAttestorPlugin(attestationPlugin);
        assertEquals(pluginName, pluginRequestMessage.getActorName().get());
    }

    @Test
    void shouldAbleToGetActorNameIfParamsIsPresent() {
        String attestationPlugin = "did:internal:ClaimPluginActor";
        pluginRequestMessage.setAttestorPlugin(attestationPlugin);
        assertEquals(pluginName, pluginRequestMessage.getActorName().get());
    }

    @Test
    void shouldReturnOptionalEmptyForInvalidPluginURI() {
        String attestationPlugin = "did:internal";
        pluginRequestMessage.setAttestorPlugin(attestationPlugin);
        assertEquals(Optional.empty(), pluginRequestMessage.getActorName());
    }
}