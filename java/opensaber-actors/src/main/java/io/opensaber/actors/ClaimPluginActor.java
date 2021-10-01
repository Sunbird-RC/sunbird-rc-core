package io.opensaber.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.pojos.PluginRequestMessage;
import io.opensaber.pojos.dto.ClaimDTO;
import org.springframework.web.client.RestTemplate;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;

import java.util.HashMap;

public class ClaimPluginActor extends BaseActor {

    @Override
    protected void onReceive(MessageProtos.Message request) throws Throwable {
        // TODO: remove the property URI totally, since we have property paths
        // TODO: fix notes and requestorName
        // TODO: return response to set the claim ID
        final String claimRequestUrl = "http://localhost:8082";
        final String CLAIMS_PATH = "/api/v1/claims";
        String payLoad = request.getPayload().getStringValue();
        PluginRequestMessage pluginRequestMessage = new ObjectMapper().readValue(payLoad, PluginRequestMessage.class);

        ClaimDTO claimDTO = new ClaimDTO();
        claimDTO.setEntity(pluginRequestMessage.getSourceEntity());
        claimDTO.setEntityId(pluginRequestMessage.getSourceOSID());
        claimDTO.setPropertyURI("");
        claimDTO.setPropertyData(pluginRequestMessage.getPropertyData().toString());
        claimDTO.setConditions(pluginRequestMessage.getConditions());
        claimDTO.setAttestorEntity(pluginRequestMessage.getConditions());
        claimDTO.setConditions(pluginRequestMessage.getConditions());
//        claimDTO.setRequestorName(requestorName);
//        claimDTO.setNotes(notes);
        RestTemplate restTemplate = new RestTemplate();
        HashMap<String, Object> hashMap = restTemplate.postForObject(claimRequestUrl + CLAIMS_PATH, claimDTO, HashMap.class);
        logger.info("Claim has successfully risen {}", hashMap.toString());
    }
}
