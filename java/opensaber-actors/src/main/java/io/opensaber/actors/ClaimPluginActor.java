package io.opensaber.actors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.actors.factory.MessageFactory;
import io.opensaber.pojos.PluginRequestMessage;
import io.opensaber.pojos.PluginResponseMessage;
import io.opensaber.pojos.PluginResponseMessageCreator;
import io.opensaber.pojos.attestation.Action;
import io.opensaber.pojos.dto.ClaimDTO;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.sunbird.akka.core.ActorCache;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;
import org.sunbird.akka.core.Router;

public class ClaimPluginActor extends BaseActor {
    private final String claimRequestUrl = "http://localhost:8082";
    private final String CLAIMS_PATH = "/api/v1/claims";
    RestTemplate restTemplate = new RestTemplate();


    @Override
    protected void onReceive(MessageProtos.Message request) throws Throwable {
        // TODO: remove the property URI totally, since we have property paths
        // TODO: fix notes and requestorName
        // TODO: return response to set the claim ID
        String payLoad = request.getPayload().getStringValue();
        PluginRequestMessage pluginRequestMessage = new ObjectMapper().readValue(payLoad, PluginRequestMessage.class);

        switch (Action.valueOf(pluginRequestMessage.getStatus())) {
            case RAISE_CLAIM:
                riseClaim(pluginRequestMessage);
                break;
            case GRANT_CLAIM:
            case REJECT_CLAIM:
                attestClaim(pluginRequestMessage);
                break;
        }
    }

    private void attestClaim(PluginRequestMessage pluginRequestMessage) throws JsonProcessingException {
        JsonNode additionalInputs = pluginRequestMessage.getAdditionalInputs();
        String claimId = additionalInputs.get("claimId").asText();
        JsonNode attestorInfo = additionalInputs.get("attestorInfo");
        String status = pluginRequestMessage.getStatus();
        String notes = additionalInputs.get("notes").asText();
        ObjectNode attestationRequest = JsonNodeFactory.instance.objectNode();
        attestationRequest.set("attestorInfo", attestorInfo);
        attestationRequest.put("action", status);
        attestationRequest.put("notes", notes);
        ResponseEntity<Object> responseEntity = restTemplate.exchange(
                claimRequestUrl + CLAIMS_PATH + "/" + claimId,
                HttpMethod.POST,
                new HttpEntity<>(attestationRequest),
                Object.class
        );
        callPluginResponseActor(pluginRequestMessage, claimId, Action.valueOf(status));
        logger.info("Claim has successfully risen {}", responseEntity.toString());
    }

    private void riseClaim(PluginRequestMessage pluginRequestMessage) throws JsonProcessingException {
        JsonNode additionalInputs = pluginRequestMessage.getAdditionalInputs();
        String notes = "";
        if(additionalInputs.has("notes")) {
            notes = additionalInputs.get("notes").asText();
        }

        ClaimDTO claimDTO = new ClaimDTO();
        claimDTO.setEntity(pluginRequestMessage.getSourceEntity());
        claimDTO.setEntityId(pluginRequestMessage.getSourceOSID());
        claimDTO.setPropertyURI("");
        claimDTO.setPropertyData(pluginRequestMessage.getPropertyData().toString());
        claimDTO.setConditions(pluginRequestMessage.getConditions());
        claimDTO.setAttestorEntity(pluginRequestMessage.getConditions());
        claimDTO.setConditions(pluginRequestMessage.getConditions());
        claimDTO.setAttestationId(pluginRequestMessage.getAttestationOSID());
        claimDTO.setNotes(notes);
//        claimDTO.setRequestorName(requestorName);

        JsonNode response = restTemplate.postForObject(claimRequestUrl + CLAIMS_PATH, claimDTO, JsonNode.class);
        logger.info("Claim has successfully risen {}", response.toString());

        String claimId = response.get("id").asText();
        callPluginResponseActor(pluginRequestMessage, claimId, Action.RAISE_CLAIM);
    }

    private void callPluginResponseActor(PluginRequestMessage pluginRequestMessage, String claimId, Action raiseClaim) throws JsonProcessingException {
        PluginResponseMessage pluginResponseMessage = PluginResponseMessageCreator.createClaimResponseMessage(claimId, raiseClaim, pluginRequestMessage);
        MessageProtos.Message pluginResponseActorMessage = MessageFactory.instance().createPluginResponseMessage(pluginResponseMessage);
        ActorCache.instance().get(Router.ROUTER_NAME).tell(pluginResponseActorMessage, null);
    }
}
