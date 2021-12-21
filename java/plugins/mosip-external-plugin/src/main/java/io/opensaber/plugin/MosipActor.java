package io.opensaber.plugin;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.opensaber.actors.factory.MessageFactory;
import io.opensaber.pojos.PluginRequestMessage;
import io.opensaber.pojos.PluginResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestTemplate;
import org.sunbird.akka.core.ActorCache;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;
import org.sunbird.akka.core.Router;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static io.opensaber.pojos.attestation.Action.*;
import static java.net.URLDecoder.decode;

public class MosipActor extends BaseActor {
    private static final Logger logger = LoggerFactory.getLogger(MosipActor.class);

    @Override
    public void onReceive(MessageProtos.Message request) throws Throwable {
        String payLoad = request.getPayload().getStringValue();
        PluginRequestMessage pluginRequestMessage = new ObjectMapper().readValue(payLoad, PluginRequestMessage.class);
        logger.info("Received request message {} ", pluginRequestMessage);
        JsonNode additionalInput = pluginRequestMessage.getAdditionalInputs();

        RestTemplate restTemplate = new RestTemplate();
        String mosipUrl = "http://localhost:5000";
        try {
            ResponseEntity<Object> responseEntity = restTemplate.postForEntity(mosipUrl, additionalInput, Object.class);
            if(!responseEntity.getStatusCode().is2xxSuccessful()) {
                logger.error(responseEntity.toString());
            }
        } catch (Exception e) {
            logger.error("Mosip call failed");
        }
    }

    @Override
    public void onFailure(MessageProtos.Message message) {
        logger.info("Send hello failed {}", message.toString());
    }

    @Override
    public void onSuccess(MessageProtos.Message message) {
        logger.info("Send hello answered successfully {}", message.toString());
    }

}
