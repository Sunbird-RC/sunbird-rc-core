package io.opensaber.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.audit.AuditServiceImpl;
import io.opensaber.audit.IAuditService;
import io.opensaber.pojos.AuditRecord;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;

public class AuditActor extends BaseActor {
    public IAuditService auditService;
    public ObjectMapper objectMapper;

    @Override
    protected void onReceive(MessageProtos.Message message) throws Throwable {
        logger.debug("Received a message to ElasticSearch Actor {}", message.getPerformOperation());
        auditService = new AuditServiceImpl();
        objectMapper = new ObjectMapper();
        AuditRecord auditMessage = objectMapper.readValue(message.getPayload().getStringValue(), AuditRecord.class);
        auditService.audit(auditMessage);
    }
}
