package io.opensaber.plugin.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.opensaber.actors.factory.MessageFactory;
import io.opensaber.pojos.PluginResponseMessage;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.sunbird.akka.core.ActorCache;
import org.sunbird.akka.core.MessageProtos;
import org.sunbird.akka.core.Router;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;

import static io.opensaber.pojos.attestation.Action.GRANT_CLAIM;

@RestController
public class MosipController {
    @RequestMapping(value = "/mosip/callback", method = RequestMethod.POST)
    public void registryHealth(HttpServletResponse httpServletResponse, @RequestBody JsonNode requestBody) throws JsonProcessingException {

        PluginResponseMessage pluginResponseMessage = PluginResponseMessage.builder()
                .additionalData(JsonNodeFactory.instance.nullNode())
                .date(new Date())
                .validUntil(new Date())
                .version("").build();
        pluginResponseMessage.setStatus(GRANT_CLAIM.name());
        String response = requestBody.toString();
        pluginResponseMessage.setResponse(response);
        MessageProtos.Message esProtoMessage = MessageFactory.instance().createPluginResponseMessage(pluginResponseMessage);
        ActorCache.instance().get(Router.ROUTER_NAME).tell(esProtoMessage, null);
    }
}
