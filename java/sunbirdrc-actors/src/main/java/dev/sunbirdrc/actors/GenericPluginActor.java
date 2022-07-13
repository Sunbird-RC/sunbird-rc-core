package dev.sunbirdrc.actors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.sunbirdrc.actors.factory.MessageFactory;
import dev.sunbirdrc.pojos.PluginRequestMessage;
import dev.sunbirdrc.pojos.PluginResponseMessage;
import dev.sunbirdrc.pojos.PluginResponseMessageCreator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.sunbird.akka.core.ActorCache;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;
import org.sunbird.akka.core.Router;

import java.util.Date;
import java.util.Objects;

import static dev.sunbirdrc.pojos.attestation.Action.GRANT_CLAIM;
import static dev.sunbirdrc.pojos.attestation.Action.REJECT_CLAIM;

public class GenericPluginActor extends BaseActor {
    @Override
    protected void onReceive(MessageProtos.Message request) throws Throwable {
        String payLoad = request.getPayload().getStringValue();
        PluginRequestMessage pluginRequestMessage = new ObjectMapper().readValue(payLoad, PluginRequestMessage.class);
        logger.info("Received request message {} ", pluginRequestMessage);
        JsonNode additionalInput = pluginRequestMessage.getAdditionalInputs();

        String url = "http://127.0.0.1:5000/mosip";
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, additionalInput, JsonNode.class);
        PluginResponseMessage pluginResponseMessage = PluginResponseMessageCreator.createPluginResponseMessage(pluginRequestMessage);

        if(response.getStatusCode().is2xxSuccessful()) {
            JsonNode responseBody = response.getBody();
            String verified = "verified";
            if(Objects.requireNonNull(responseBody).has(verified) && responseBody.get(verified).asBoolean()) {
                pluginResponseMessage.setStatus(GRANT_CLAIM.name());
                pluginResponseMessage.setResponse(responseBody.get("data").toString());
            }
        } else {
            pluginResponseMessage.setStatus(REJECT_CLAIM.name());
        }
        MessageProtos.Message esProtoMessage = MessageFactory.instance().createPluginResponseMessage(pluginResponseMessage);
        ActorCache.instance().get(Router.ROUTER_NAME).tell(esProtoMessage, null);
    }
}
