package dev.sunbirdrc.actors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.actors.factory.MessageFactory;
import dev.sunbirdrc.pojos.PluginRequestMessage;
import dev.sunbirdrc.pojos.PluginResponseMessage;
import dev.sunbirdrc.pojos.PluginResponseMessageCreator;
import dev.sunbirdrc.pojos.attestation.Action;
import dev.sunbirdrc.pojos.dto.ClaimDTO;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.sunbird.akka.core.ActorCache;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;
import org.sunbird.akka.core.Router;

import java.io.IOException;
import java.util.Objects;

import static dev.sunbirdrc.registry.middleware.util.Constants.USER_ID;
import static org.apache.commons.lang3.StringUtils.isBlank;


public class ClaimPluginActor extends BaseActor {
    // TODO: read url from config
    private final String claimRequestUrl = System.getenv().getOrDefault("claims_url", "http://localhost:8082");
    private final String CLAIMS_PATH = "/api/v1/claims";
    RestTemplate restTemplate = new RestTemplate();


    @Override
    protected void onReceive(MessageProtos.Message request) throws Throwable {
        // TODO: remove the property URI totally, since we have property paths
        // TODO: fix notes and requestorName
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

    private void attestClaim(PluginRequestMessage pluginRequestMessage) throws IOException {
        JsonNode additionalInputs = pluginRequestMessage.getAdditionalInputs();
        String claimId = additionalInputs.get("claimId").asText();
        JsonNode attestorInfo = additionalInputs.get("attestorInfo");
        String status = pluginRequestMessage.getStatus();
        String notes = additionalInputs.get("notes").asText();
        ObjectNode attestationRequest = JsonNodeFactory.instance.objectNode();
        attestationRequest.set("attestorInfo", attestorInfo);
        attestationRequest.put("action", status);
        attestationRequest.put("notes", notes);
        attestationRequest.put(USER_ID, pluginRequestMessage.getUserId());
        ResponseEntity<ClaimDTO> responseEntity = restTemplate.exchange(
                getClaimRequestUrl() + CLAIMS_PATH + "/" + claimId,
                HttpMethod.POST,
                new HttpEntity<>(attestationRequest),
                ClaimDTO.class
        );
        ClaimDTO claimDTO = Objects.requireNonNull(responseEntity.getBody());
        pluginRequestMessage.setSourceEntity(claimDTO.getEntity());
        pluginRequestMessage.setSourceOSID(claimDTO.getEntityId());
        pluginRequestMessage.setAttestationOSID(claimDTO.getAttestationId());
        pluginRequestMessage.setPolicyName(claimDTO.getAttestationName());
        pluginRequestMessage.setPropertyData(claimDTO.getPropertyData());
        callPluginResponseActor(pluginRequestMessage, claimId, Action.valueOf(status));
        logger.info("Claim has successfully attested {}", responseEntity.toString());
    }

    private String getClaimRequestUrl() {
        if (isBlank(claimRequestUrl)) {
            logger.error("claims service url is not set but it seems to be in use.");
        }
        return claimRequestUrl;
    }

    private void riseClaim(PluginRequestMessage pluginRequestMessage) throws IOException {
        JsonNode additionalInputs = pluginRequestMessage.getAdditionalInputs();
        String notes = "";
        if(additionalInputs.has("notes")) {
            notes = additionalInputs.get("notes").asText();
        }

        ClaimDTO claimDTO = new ClaimDTO();
        claimDTO.setEntity(pluginRequestMessage.getSourceEntity());
        claimDTO.setEntityId(pluginRequestMessage.getSourceOSID());
        claimDTO.setPropertyURI("");
        claimDTO.setPropertyData(pluginRequestMessage.getPropertyData());
        claimDTO.setConditions(pluginRequestMessage.getConditions());
        claimDTO.setAttestorEntity(pluginRequestMessage.getAttestorEntity());
        claimDTO.setAttestationId(pluginRequestMessage.getAttestationOSID());
        claimDTO.setNotes(notes);
        claimDTO.setAttestationName(pluginRequestMessage.getPolicyName());
        claimDTO.setRequestorName(pluginRequestMessage.getUserId());

        JsonNode response = restTemplate.postForObject(getClaimRequestUrl() + CLAIMS_PATH, claimDTO, JsonNode.class);
        logger.info("Claim has successfully risen {}", response.toString());

        String claimId = response.get("id").asText();
        callPluginResponseActor(pluginRequestMessage, claimId, Action.RAISE_CLAIM);
    }

    private void callPluginResponseActor(PluginRequestMessage pluginRequestMessage, String claimId, Action raiseClaim) throws IOException {
        PluginResponseMessage pluginResponseMessage = PluginResponseMessageCreator.createClaimResponseMessage(claimId, raiseClaim, pluginRequestMessage);
        if(Action.valueOf(pluginRequestMessage.getStatus()).equals(Action.GRANT_CLAIM)) {
            pluginResponseMessage.setResponse(pluginRequestMessage.getPropertyData());
        }
        MessageProtos.Message pluginResponseActorMessage = MessageFactory.instance().createPluginResponseMessage(pluginResponseMessage);
        ActorCache.instance().get(Router.ROUTER_NAME).tell(pluginResponseActorMessage, null);
    }
}
