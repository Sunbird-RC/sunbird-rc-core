package dev.sunbirdrc.registry.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
public class NotificationTemplates {
    private List<NotificationTemplate> createNotificationTemplates;
    private List<NotificationTemplate> updateNotificationTemplates;
    private List<NotificationTemplate> inviteNotificationTemplates;
    private List<NotificationTemplate> deleteNotificationTemplates;

    public NotificationTemplates() {
        NotificationTemplate template = new NotificationTemplate("Credential Created", "{\"sender\": \"AppName\",\"route\": \"4\",\"country\": \"91\",\"unicode\": 1,\"sms\": [{    \"message\": \"{{name}}, Your {{entityType}} credential has been created\",    \"to\": [      \"{{contact}}\"    ]  }],\"DLT_TE_ID\": \"templateId\"}");
        createNotificationTemplates = new ArrayList();
        createNotificationTemplates.add(template);

        template = new NotificationTemplate("Credential Updated", "{\"sender\": \"AppName\",\"route\": \"4\",\"country\": \"91\",\"unicode\": 1,\"sms\": {    \"message\": \"{{name}}, Your {{entityType}} credential has been updated\",    \"to\": [      \"{{contact}}\"    ]  }],\"DLT_TE_ID\": \"templateId\"}");
        updateNotificationTemplates = new ArrayList();
        updateNotificationTemplates.add(template);

        template = new NotificationTemplate("Invitation", "{\"sender\": \"AppName\",\"route\": \"4\",\"country\": \"91\",\"unicode\": 1,\"sms\": [  {    \"message\": \"{{name}}, You have been invited for {{entityType}}\",    \"to\": [      \"{{contact}}\"    ]  }],\"DLT_TE_ID\": \"templateId\"}");
        inviteNotificationTemplates = new ArrayList<>();
        inviteNotificationTemplates.add(template);

        template = new NotificationTemplate("Revoked", "{\"sender\": \"AppName\",\"route\": \"4\",\"country\": \"91\",\"unicode\": 1,\"sms\": [  {    \"message\": \"{{name}}, Your {{entityType}} credential has been revoked\",    \"to\": [      \"{{contact}}\"    ]  }],\"DLT_TE_ID\": \"templateId\"}");
        deleteNotificationTemplates = new ArrayList<>();
        deleteNotificationTemplates.add(template);
    }
}
