package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import dev.sunbirdrc.registry.helper.EntityStateHelper;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import dev.sunbirdrc.registry.util.OSSchemaConfiguration;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

import static dev.sunbirdrc.registry.Constants.*;
import static dev.sunbirdrc.registry.middleware.util.Constants.EMAIL;
import static dev.sunbirdrc.registry.middleware.util.Constants.MOBILE;

@Component
public class NotificationHelper {
    private static Logger logger = LoggerFactory.getLogger(NotificationHelper.class);
    @Value("${notification.service.enabled}") boolean notificationEnabled;
    @Autowired
    private IDefinitionsManager definitionsManager;
    @Autowired
    @Setter
    private EntityStateHelper entityStateHelper;
    @Autowired
    private RegistryService registryService;

    @Autowired
    @Setter
    private ObjectMapper objectMapper;

    public void sendNotification(JsonNode inputJson, String operationType) throws Exception {
        String entityType = inputJson.fields().next().getKey();
        String messageTemplateBody = getNotificationTemplateBody(entityType, operationType);
        String messageTemplateSubject = getNotificationTemplateSubject(entityType, operationType);
        Map<String, Object> objectNodeMap = convertEntityToMap(inputJson, entityType);
        String subjectString = getMessageString(messageTemplateSubject, objectNodeMap, operationType);
        String bodyString = getMessageString(messageTemplateBody, objectNodeMap, operationType);
        sendNotificationToOwners(inputJson, operationType, subjectString, bodyString);
    }

    private String getNotificationTemplateBody(String entityType, String operationType) {
        OSSchemaConfiguration osSchemaConfiguration = definitionsManager.getDefinition(entityType).getOsSchemaConfiguration();
        switch(operationType) {
            case CREATE:
                return osSchemaConfiguration.getCreateNotificationBodyTemplate();
            case UPDATE:
                return osSchemaConfiguration.getUpdateNotificationBodyTemplate();
            case INVITE:
                return osSchemaConfiguration.getInviteNotificationBodyTemplate();
        }
        return null;
    }

    private String getNotificationTemplateSubject(String entityType, String operationType) {
        OSSchemaConfiguration osSchemaConfiguration = definitionsManager.getDefinition(entityType).getOsSchemaConfiguration();
        switch(operationType) {
            case CREATE:
                return osSchemaConfiguration.getCreateNotificationSubjectTemplate();
            case UPDATE:
                return osSchemaConfiguration.getUpdateNotificationSubjectTemplate();
            case INVITE:
                return osSchemaConfiguration.getInviteNotificationSubjectTemplate();
        }
        return null;
    }

    private Map<String, Object> convertEntityToMap(JsonNode inputJson, String entityName) throws JsonProcessingException {
        final String osSignedData = "_osSignedData";
        final String entityType = "entityType";
        if(inputJson.get(entityName).has(osSignedData)) {
            JsonNode signedDataNode = objectMapper.readTree(inputJson.get(entityName).get(osSignedData).asText());
            inputJson = ((ObjectNode)inputJson.get(entityName)).set(osSignedData, signedDataNode);
        }
        Map<String, Object> objectNodeMap = objectMapper.convertValue(inputJson, new TypeReference<Map<String, Object>>(){});
        objectNodeMap.put(entityType, entityName);
        return objectNodeMap;
    }

    private String getMessageString(String messageTemplateBody, Map<String, Object> objectNodeMap, String operationType) throws IOException {
        Handlebars handlebars = new Handlebars();
        Template messageTemplate = handlebars.compileInline(messageTemplateBody);
        return messageTemplate.apply(objectNodeMap);
    }
    private void sendNotificationToOwners(JsonNode inputJson, String operation, String subject, String message) throws Exception {
        if (notificationEnabled) {
            String entityType = inputJson.fields().next().getKey();
            for (ObjectNode owners : entityStateHelper.getOwnersData(inputJson, entityType)) {
                String ownerMobile = owners.get(MOBILE).asText("");
                String ownerEmail = owners.get(EMAIL).asText("");
                if (!StringUtils.isEmpty(ownerMobile)) {
                    registryService.callNotificationActors(operation, String.format("tel:%s", ownerMobile), subject, message);
                }
                if (!StringUtils.isEmpty(ownerEmail)) {
                    registryService.callNotificationActors(operation, String.format("mailto:%s", ownerEmail), subject, message);
                }
            }
        }
    }
}
