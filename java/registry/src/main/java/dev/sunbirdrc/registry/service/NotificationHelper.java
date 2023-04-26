package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import dev.sunbirdrc.registry.helper.EntityStateHelper;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import dev.sunbirdrc.registry.util.OSSchemaConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static dev.sunbirdrc.registry.Constants.*;
import static dev.sunbirdrc.registry.middleware.util.Constants.EMAIL;
import static dev.sunbirdrc.registry.middleware.util.Constants.MOBILE;

@Service
public class NotificationHelper {
    private static Logger logger = LoggerFactory.getLogger(NotificationHelper.class);
    boolean notificationEnabled;
    private IDefinitionsManager definitionsManager;
    private EntityStateHelper entityStateHelper;
    private RegistryService registryService;
    private ObjectMapper objectMapper;
    @Autowired
    public NotificationHelper(@Value("${notification.service.enabled}") boolean notificationEnabled, IDefinitionsManager definitionsManager, EntityStateHelper entityStateHelper, RegistryService registryService, ObjectMapper objectMapper) {
        this.notificationEnabled = notificationEnabled;
        this.definitionsManager = definitionsManager;
        this.entityStateHelper = entityStateHelper;
        this.registryService = registryService;
        this.objectMapper = objectMapper;
    }

    public NotificationHelper() {
    }

    public void sendNotification(JsonNode inputJson, String operationType) throws Exception {
        String entityType = inputJson.fields().next().getKey();
        String messageBodySubject = getNotificationBodyTemplate(entityType, operationType);
        String messageSubjectTemplate = getNotificationSubjectTemplate(entityType, operationType);
        Map<String, Object> objectNodeMap = JSONUtil.convertJsonNodeToMap(inputJson);
        objectNodeMap.put("entityType", entityType);
        String subjectString = compileMessageFromTemplate(messageSubjectTemplate, objectNodeMap);
        String bodyString = compileMessageFromTemplate(messageBodySubject, objectNodeMap);
        List<ObjectNode> owners = entityStateHelper.getOwnersData(inputJson, entityType);
        sendNotificationToOwners(owners, operationType, subjectString, bodyString);
    }

    private void sendNotificationToOwners(List<ObjectNode> owners, String operation, String subject, String message) throws Exception {
        if (notificationEnabled) {
            for (ObjectNode owner :owners) {
                String ownerMobile = owner.get(MOBILE).asText("");
                String ownerEmail = owner.get(EMAIL).asText("");
                if (!StringUtils.isEmpty(ownerMobile)) {
                    registryService.callNotificationActors(operation, String.format("tel:%s", ownerMobile), subject, message);
                }
                if (!StringUtils.isEmpty(ownerEmail)) {
                    registryService.callNotificationActors(operation, String.format("mailto:%s", ownerEmail), subject, message);
                }
            }
        }
    }
    private String getNotificationBodyTemplate(String entityType, String operationType) {
        OSSchemaConfiguration osSchemaConfiguration = definitionsManager.getDefinition(entityType).getOsSchemaConfiguration();
        switch(operationType) {
            case CREATE:
                return osSchemaConfiguration.getNotificationTemplates().getCreateNotificationTemplates().getBody();
            case UPDATE:
                return osSchemaConfiguration.getNotificationTemplates().getUpdateNotificationTemplates().getBody();
            case INVITE:
                return osSchemaConfiguration.getNotificationTemplates().getInviteNotificationTemplates().getBody();
            case DELETE:
                return osSchemaConfiguration.getNotificationTemplates().getDeleteNotificationTemplates().getBody();
        }
        return null;
    }

    private String getNotificationSubjectTemplate(String entityType, String operationType) {
        OSSchemaConfiguration osSchemaConfiguration = definitionsManager.getDefinition(entityType).getOsSchemaConfiguration();
        switch(operationType) {
            case CREATE:
                return osSchemaConfiguration.getNotificationTemplates().getCreateNotificationTemplates().getSubject();
            case UPDATE:
                return osSchemaConfiguration.getNotificationTemplates().getUpdateNotificationTemplates().getSubject();
            case INVITE:
                return osSchemaConfiguration.getNotificationTemplates().getInviteNotificationTemplates().getSubject();
            case DELETE:
                return osSchemaConfiguration.getNotificationTemplates().getDeleteNotificationTemplates().getSubject();
        }
        return null;
    }

    private String compileMessageFromTemplate(String messageBodySubject, Map<String, Object> objectNodeMap) throws IOException {
        Handlebars handlebars = new Handlebars();
        Template messageTemplate = handlebars.compileInline(messageBodySubject);
        return messageTemplate.apply(objectNodeMap);
    }

}
