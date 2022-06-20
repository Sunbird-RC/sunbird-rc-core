package dev.sunbirdrc.plugin;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.actors.factory.MessageFactory;
import dev.sunbirdrc.plugin.components.SpringContext;
import dev.sunbirdrc.plugin.dto.VerifyRequest;
import dev.sunbirdrc.plugin.services.DivocServices;
import dev.sunbirdrc.pojos.PluginFile;
import dev.sunbirdrc.pojos.PluginRequestMessage;
import dev.sunbirdrc.pojos.PluginResponseMessage;
import dev.sunbirdrc.pojos.PluginResponseMessageCreator;
import dev.sunbirdrc.pojos.attestation.Action;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.sunbird.akka.core.ActorCache;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;
import org.sunbird.akka.core.Router;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

public class DivocPDFActor extends BaseActor {
	private static final String VERIFY_URL = System.getenv("verify_url");
	private static final String PUBLIC_KEY = System.getenv("divoc_public_key");
	private static final String SIGNED_KEY_TYPE = System.getenv("divoc_key_type");
	private ObjectMapper objectMapper;


	public DivocPDFActor() {
		this.objectMapper = new ObjectMapper();

	}

	@Override
	public void onReceive(MessageProtos.Message request) throws Throwable {
		logger.debug("Received a message to Notification Actor {}", request.getPerformOperation());
		objectMapper = new ObjectMapper();
		ApplicationContext context = SpringContext.getAppContext();
		// get instance of MainSpringClass (Spring Managed class)
		DivocServices divocServices = (DivocServices) context.getBean("divocServices");
		PluginRequestMessage pluginRequestMessage = objectMapper.readValue(request.getPayload().getStringValue(), PluginRequestMessage.class);
		String clientToken = divocServices.fetchClientToken();
		String propertyData = pluginRequestMessage.getPropertyData();
		Map propertyDataMap = objectMapper.readValue(propertyData, Map.class);
		byte[] fileBytes = divocServices.fetchDivocPdf(clientToken, pluginRequestMessage.getAdditionalInputs().get("preEnrollmentCode").textValue(), propertyDataMap.get("mobileNumber").toString());
		PluginResponseMessage pluginResponseMessage = PluginResponseMessageCreator.createPluginResponseMessage(pluginRequestMessage);
		if (fileBytes != null) {
			pluginResponseMessage.setFiles(Collections.singletonList(PluginFile.builder().file(fileBytes)
					.fileName(String.format("%s.pdf", pluginRequestMessage.getAttestationOSID())).build()));
			pluginResponseMessage.setStatus(Action.GRANT_CLAIM.name());
			ObjectNode responseNode = objectMapper.createObjectNode();
			responseNode.set("status", JsonNodeFactory.instance.textNode("true"));
			pluginResponseMessage.setResponse(responseNode.toString());
		} else {
			pluginResponseMessage.setStatus(Action.REJECT_CLAIM.name());
			pluginResponseMessage.setResponse("No certificates found");
		}
		logger.info("{}", pluginRequestMessage);
		MessageProtos.Message esProtoMessage = MessageFactory.instance().createPluginResponseMessage(pluginResponseMessage);
		ActorCache.instance().get(Router.ROUTER_NAME).tell(esProtoMessage, null);
	}

	@Override
	public void onFailure(MessageProtos.Message message) {
		logger.info("Send hello failed {}", message.toString());
	}

	@Override
	public void onSuccess(MessageProtos.Message message) {
		logger.info("Send hello answered successfully {}", message.toString());
	}

	private Map callW3cVerifyAPI(VerifyRequest verifyRequest) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<VerifyRequest> entity = new HttpEntity<>(verifyRequest, headers);
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<Map> responseEntity = restTemplate.exchange(VERIFY_URL, HttpMethod.POST, entity, Map.class);
		logger.info("Verification api call's status {}", responseEntity.getStatusCode());
		return responseEntity.getBody();
	}

}