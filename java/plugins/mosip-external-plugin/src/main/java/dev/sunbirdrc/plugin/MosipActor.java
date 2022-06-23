package dev.sunbirdrc.plugin;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.plugin.components.SpringContext;
import dev.sunbirdrc.plugin.dto.FetchCredentialsDto;
import dev.sunbirdrc.plugin.services.MosipServices;
import dev.sunbirdrc.pojos.PluginRequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;

public class MosipActor extends BaseActor {
    private static final Logger logger = LoggerFactory.getLogger(MosipActor.class);

    @Override
    public void onReceive(MessageProtos.Message request) throws Throwable {
        ApplicationContext context = SpringContext.getAppContext();
        // get instance of MainSpringClass (Spring Managed class)
        MosipServices mosipServices = (MosipServices)context.getBean("mosipServices");
        String payLoad = request.getPayload().getStringValue();
        PluginRequestMessage pluginRequestMessage = new ObjectMapper().readValue(payLoad, PluginRequestMessage.class);
        logger.info("Received request message {} ", pluginRequestMessage);
        JsonNode additionalInput = pluginRequestMessage.getAdditionalInputs();
        FetchCredentialsDto fetchCredentialsDto = FetchCredentialsDto.builder()
                .osid(pluginRequestMessage.getSourceOSID())
                .attestationOsid(pluginRequestMessage.getAttestationOSID())
                .uid(additionalInput.get("uid").asText())
                .otp(additionalInput.get("otp").asText())
                .transactionId(additionalInput.get("transactionId").asText())
                .build();
        mosipServices.fetchCredentials(fetchCredentialsDto);
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
