package dev.sunbirdrc.actors.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Value;
import dev.sunbirdrc.elastic.ESMessage;
import dev.sunbirdrc.pojos.*;
import dev.sunbirdrc.pojos.PluginRequestMessage;
import dev.sunbirdrc.pojos.attestation.auto.AutoAttestationMessage;
import dev.sunbirdrc.pojos.attestation.auto.AutoAttestationPolicy;
import dev.sunbirdrc.pojos.attestation.exception.PolicyNotFoundException;
import dev.sunbirdrc.registry.middleware.util.Constants;
import org.sunbird.akka.core.MessageProtos;

import java.util.HashMap;
import java.util.Map;

public class MessageFactory {
    private static final MessageFactory instance = new MessageFactory();

    private MessageFactory() {
    }

    public static MessageFactory instance() {
        return instance;
    }

    public MessageProtos.Message createElasticSearchMessage(String operation, ESMessage esMessage) throws JsonProcessingException {
        MessageProtos.Message.Builder msgBuilder = MessageProtos.Message.newBuilder();
        msgBuilder.setPerformOperation(operation);
        msgBuilder.setTargetActorName(Constants.ELASTIC_SEARCH_ACTOR);
        Value.Builder payloadBuilder = msgBuilder.getPayloadBuilder();
        ObjectMapper objectMapper = new ObjectMapper();
        payloadBuilder.setStringValue(objectMapper.writeValueAsString(esMessage));
        msgBuilder.setPayload(payloadBuilder.build());
        return msgBuilder.build();
    }

    public MessageProtos.Message createAuditMessage(AuditRecord auditRecord) throws JsonProcessingException {
        MessageProtos.Message.Builder msgBuilder = MessageProtos.Message.newBuilder();
        msgBuilder.setTargetActorName(Constants.AUDIT_ACTOR);
        Value.Builder payloadBuilder = msgBuilder.getPayloadBuilder();
        ObjectMapper objectMapper = new ObjectMapper();
        payloadBuilder.setStringValue(objectMapper.writeValueAsString(auditRecord));
        msgBuilder.setPayload(payloadBuilder.build());
        return msgBuilder.build();
    }

    public MessageProtos.Message createOSActorMessage(boolean esEnabled, String operation, String index, String uuidPropertyValue, JsonNode latestNode,
                                                      AuditRecord auditRecord) throws JsonProcessingException {
        MessageProtos.Message.Builder msgBuilder = MessageProtos.Message.newBuilder();
        msgBuilder.setPerformOperation(operation);
        msgBuilder.setTargetActorName(Constants.OS_ACTOR);
        Value.Builder payloadBuilder = msgBuilder.getPayloadBuilder();
        ESMessage esMessage = new ESMessage();
        esMessage.setIndexName(index);
        esMessage.setUuidPropertyValue(uuidPropertyValue);
        esMessage.setInput(latestNode);
        ObjectMapper objectMapper = new ObjectMapper();
        OSEvent osEvent = new OSEvent();
        Map<String, Object> osMsg = new HashMap<>();
        osMsg.put("esEnabled", esEnabled);
        osMsg.put("esMessage", esMessage);
        osMsg.put("auditMessage", auditRecord);
        osEvent.setOsMap(osMsg);
        payloadBuilder.setStringValue(objectMapper.writeValueAsString(osEvent));
        msgBuilder.setPayload(payloadBuilder.build());
        return msgBuilder.build();
    }

    public MessageProtos.Message createPluginActorMessage(String pluginActorName, PluginRequestMessage pluginRequestMessage) throws JsonProcessingException {
        MessageProtos.Message.Builder msgBuilder = MessageProtos.Message.newBuilder();
        msgBuilder.setTargetActorName(pluginActorName);
        Value.Builder payloadBuilder = msgBuilder.getPayloadBuilder();
        ObjectMapper objectMapper = new ObjectMapper();
        payloadBuilder.setStringValue(objectMapper.writeValueAsString(pluginRequestMessage));
        msgBuilder.setPayload(payloadBuilder.build());
        return msgBuilder.build();
    }

    public MessageProtos.Message createNotificationActorMessage(String operation, String to, String subject, String message) throws JsonProcessingException {
        MessageProtos.Message.Builder msgBuilder = MessageProtos.Message.newBuilder();
        msgBuilder.setPerformOperation(operation);
        msgBuilder.setTargetActorName(Constants.NOTIFICATION_ACTOR);
        Value.Builder payloadBuilder = msgBuilder.getPayloadBuilder();
        ObjectMapper objectMapper = new ObjectMapper();
        NotificationMessage notificationMessage = new NotificationMessage();
        notificationMessage.setMessage(message);
        notificationMessage.setTo(to);
        notificationMessage.setSubject(subject);
        payloadBuilder.setStringValue(objectMapper.writeValueAsString(notificationMessage));
        msgBuilder.setPayload(payloadBuilder.build());
        return msgBuilder.build();
    }

    public MessageProtos.Message createAutoAttestationMessage(AutoAttestationPolicy autoAttestationPolicy, JsonNode updatedNode, String accessToken, String url) throws JsonProcessingException {
        MessageProtos.Message.Builder msgBuilder = MessageProtos.Message.newBuilder();
        msgBuilder.setPerformOperation("");
        msgBuilder.setTargetActorName(Constants.AUTO_ATTESTOR_ACTOR);
        Value.Builder payloadBuilder = msgBuilder.getPayloadBuilder();
        ObjectMapper objectMapper = new ObjectMapper();
        AutoAttestationMessage autoAttestationMessage = new AutoAttestationMessage();
        autoAttestationMessage.setAutoAttestationPolicy(autoAttestationPolicy);
        autoAttestationMessage.setInput(updatedNode);
        autoAttestationMessage.setUrl(url);
        autoAttestationMessage.setAccessToken(accessToken);
        payloadBuilder.setStringValue(objectMapper.writeValueAsString(autoAttestationMessage));
        msgBuilder.setPayload(payloadBuilder.build());
        return msgBuilder.build();
    }

    public MessageProtos.Message createPluginResponseMessage(PluginResponseMessage pluginResponseMessage) throws JsonProcessingException {
        MessageProtos.Message.Builder msgBuilder = MessageProtos.Message.newBuilder();
        msgBuilder.setTargetActorName(Constants.PLUGIN_RESPONSE_ACTOR);
        Value.Builder payloadBuilder = msgBuilder.getPayloadBuilder();
        ObjectMapper objectMapper = new ObjectMapper();
        payloadBuilder.setStringValue(objectMapper.writeValueAsString(pluginResponseMessage));
        msgBuilder.setPayload(payloadBuilder.build());
        return msgBuilder.build();
    }

    public MessageProtos.Message createPluginMessage(PluginRequestMessage requestMessage) throws Exception {
        MessageProtos.Message.Builder msgBuilder = MessageProtos.Message.newBuilder();
        msgBuilder.setPerformOperation("");
        msgBuilder.setTargetActorName(requestMessage.getActorName().orElseThrow(() ->
                new Exception("Invalid plugin name " + requestMessage.getAttestorPlugin())));
        Value.Builder payloadBuilder = msgBuilder.getPayloadBuilder();
        ObjectMapper objectMapper = new ObjectMapper();
        payloadBuilder.setStringValue(objectMapper.writeValueAsString(requestMessage));
        return msgBuilder.build();
    }
}
