package dev.sunbirdrc.pojos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PluginRequestMessageTest {

    PluginRequestMessage pluginRequestMessage;
    final String pluginName = "ClaimPluginActor";

    @BeforeEach
    public void setUp() throws Exception {
        pluginRequestMessage = new PluginRequestMessage();
    }

    @Test
    public void shouldAbleToGetActorNameIfParamsIsNotPresent() {
        String attestationPlugin = "did:internal:ClaimPluginActor?entity=Teacher";
        pluginRequestMessage.setAttestorPlugin(attestationPlugin);
        assertEquals(pluginName, pluginRequestMessage.getActorName().get());
    }

    @Test
    public void shouldAbleToGetActorNameIfParamsIsPresent() {
        String attestationPlugin = "did:internal:ClaimPluginActor";
        pluginRequestMessage.setAttestorPlugin(attestationPlugin);
        assertEquals(pluginName, pluginRequestMessage.getActorName().get());
    }

    @Test
    public void shouldReturnOptionalEmptyForInvalidPluginURI() {
        String attestationPlugin = "did:internal";
        pluginRequestMessage.setAttestorPlugin(attestationPlugin);
        assertEquals(Optional.empty(), pluginRequestMessage.getActorName());
    }
}