package dev.sunbirdrc.plugin.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.sunbirdrc.actors.factory.MessageFactory;
import dev.sunbirdrc.plugin.constant.Constants;
import dev.sunbirdrc.plugin.services.MosipServices;
import dev.sunbirdrc.pojos.PluginFile;
import dev.sunbirdrc.pojos.PluginResponseMessage;
import dev.sunbirdrc.pojos.attestation.Action;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sunbird.akka.core.ActorCache;
import org.sunbird.akka.core.MessageProtos;
import org.sunbird.akka.core.Router;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

@ConditionalOnExpression("#{(environment.MOSIP_ENABLED?:'false').equals('true')}")
@RestController
public class MosipCallbackController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MosipCallbackController.class);

    @Autowired
    private MosipServices mosipServices;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${database.uuid-property-name}")
    private String uuidPropertyName;

    @RequestMapping(value = "/plugin/mosip/callback", method = RequestMethod.POST)
    public ResponseEntity callbackHandler(@RequestHeader Map<String, String> headers, @RequestParam Map queryParams, @RequestBody String request) throws JsonProcessingException {
        try {
            LOGGER.info("Mosip callback received:");
            LOGGER.info("Headers: {}, QueryParams: {}", headers, queryParams);
            JsonNode requestBody = objectMapper.readTree(request);
            byte[] bytes = mosipServices.fetchMosipPdf(headers, request);
            if (bytes != null) {

                String attestationUUID = requestBody.get("event").get("data").get("attestationOsid").asText();
                PluginResponseMessage pluginResponseMessage = PluginResponseMessage.builder().policyName("attestation-MOSIP")
                        .sourceEntity("User")
                        .sourceUUID(requestBody.get("event").get("data").get(uuidPropertyName).asText())
                        .attestationUUID(attestationUUID)
                        .attestorPlugin("did:external:MosipActor")
                        .additionalData(JsonNodeFactory.instance.nullNode())
                        .date(new Date())
                        .validUntil(new Date())
                        .version("").files(Collections.singletonList(PluginFile.builder().file(bytes).fileName(String.format("%s.pdf",attestationUUID)).build())).build();
                pluginResponseMessage.setStatus(Action.GRANT_CLAIM.name());
                pluginResponseMessage.setResponse(requestBody.get("event").get("data").toString());
                LOGGER.info("{}", pluginResponseMessage);
                MessageProtos.Message esProtoMessage = MessageFactory.instance().createPluginResponseMessage(pluginResponseMessage);
                ActorCache.instance().get(Router.ROUTER_NAME).tell(esProtoMessage, null);
                return ResponseEntity.ok(queryParams.get(Constants.HUB_CHALLENGE));
            } else {
                LOGGER.error("Failed fetching mosip pdf");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(queryParams.get(Constants.HUB_CHALLENGE));
            }
        } catch (Exception e) {
            LOGGER.error("Failed fetching mosip pdf: {}", ExceptionUtils.getStackTrace(e));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(queryParams.get(Constants.HUB_CHALLENGE));
        }
    }

    @RequestMapping(value = "/plugin/mosip/callback", method = RequestMethod.GET)
    public ResponseEntity callbackHandlerGET(@RequestParam Map queryParams) throws JsonProcessingException {
        return ResponseEntity.ok(queryParams.get(Constants.HUB_CHALLENGE));
    }
}
