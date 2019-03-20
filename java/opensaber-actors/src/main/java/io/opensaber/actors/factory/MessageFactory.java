package io.opensaber.actors.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Value;
import io.opensaber.elastic.ESMessage;
import io.opensaber.pojos.AuditRecord;
import io.opensaber.pojos.OSEvent;
import io.opensaber.registry.middleware.util.Constants;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.akka.core.MessageProtos;

public class MessageFactory {
    private static final MessageFactory instance = new MessageFactory();
    private MessageFactory() {}
    public static MessageFactory instance(){
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

    public MessageProtos.Message createOSActorMessage(boolean esEnabled, String operation, String index, String osid, JsonNode latestNode,
                                                      AuditRecord auditRecord) throws JsonProcessingException {
        MessageProtos.Message.Builder msgBuilder = MessageProtos.Message.newBuilder();
        msgBuilder.setPerformOperation(operation);
        msgBuilder.setTargetActorName(Constants.OS_ACTOR);
        Value.Builder payloadBuilder = msgBuilder.getPayloadBuilder();
        ESMessage esMessage = new ESMessage();
        esMessage.setIndexName(index);
        esMessage.setOsid(osid);
        esMessage.setInput(latestNode);
        ObjectMapper objectMapper = new ObjectMapper();
        OSEvent osEvent = new OSEvent();
        Map<String, Object> osMsg = new HashMap<>();
        osMsg.put("esEnabled",esEnabled);
        osMsg.put("esMessage",esMessage);
        osMsg.put("auditMessage",auditRecord);
        osEvent.setOsMap(osMsg);
        payloadBuilder.setStringValue(objectMapper.writeValueAsString(osEvent));
        msgBuilder.setPayload(payloadBuilder.build());
        return msgBuilder.build();
    }
}
