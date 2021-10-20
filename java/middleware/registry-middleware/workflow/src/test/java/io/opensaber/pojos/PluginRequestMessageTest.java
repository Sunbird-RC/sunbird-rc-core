package io.opensaber.pojos;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

public class PluginRequestMessageTest {

    PluginRequestMessage pluginRequestMessage;
    final String pluginName = "ClaimPluginActor";
    @Before
    public void setUp() throws Exception {
        pluginRequestMessage = new PluginRequestMessage();
    }

    @Test
    public void shouldAbleToGetActorNameIfParamsIsNotPresent() {
        String attestationPlugin = "did:internal:ClaimPluginActor?entity=Teacher";
        pluginRequestMessage.setAttestorPlugin(attestationPlugin);
        Assert.assertEquals(pluginName, pluginRequestMessage.getActorName().get());
    }

    @Test
    public void shouldAbleToGetActorNameIfParamsIsPresent() {
        String attestationPlugin = "did:internal:ClaimPluginActor";
        pluginRequestMessage.setAttestorPlugin(attestationPlugin);
        Assert.assertEquals(pluginName, pluginRequestMessage.getActorName().get());
    }

    @Test
    public void shouldReturnOptionalEmptyForInvalidPluginURI() {
        String attestationPlugin = "did:internal";
        pluginRequestMessage.setAttestorPlugin(attestationPlugin);
        Assert.assertEquals(Optional.empty(), pluginRequestMessage.getActorName());
    }
}