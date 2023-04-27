package dev.sunbirdrc.registry.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NotificationTemplates {
    private NotificationTemplate createNotificationTemplates = new NotificationTemplate("Credential Created", "{\"sender\": \"AppName\",\"route\": \"4\",\"country\": \"91\",\"unicode\": 1,\"sms\": [  {    \"message\": \"{{name}}, Your {{entityType}} credential has been created\",    \"to\": [      \"{{contact}}\"    ]  }],\"DLT_TE_ID\": \"templateId\"}");
    private NotificationTemplate updateNotificationTemplates = new NotificationTemplate("Credential Updated", "{\"sender\": \"AppName\",\"route\": \"4\",\"country\": \"91\",\"unicode\": 1,\"sms\": [  {    \"message\": \"{{name}}, Your {{entityType}} credential has been updated\",    \"to\": [      \"{{contact}}\"    ]  }],\"DLT_TE_ID\": \"templateId\"}");
    private NotificationTemplate inviteNotificationTemplates = new NotificationTemplate("Invitation", "{\"sender\": \"AppName\",\"route\": \"4\",\"country\": \"91\",\"unicode\": 1,\"sms\": [  {    \"message\": \"{{name}}, You have been invited for {{entityType}}\",    \"to\": [      \"{{contact}}\"    ]  }],\"DLT_TE_ID\": \"templateId\"}");
    private NotificationTemplate deleteNotificationTemplates = new NotificationTemplate("Revoked", "{\"sender\": \"AppName\",\"route\": \"4\",\"country\": \"91\",\"unicode\": 1,\"sms\": [  {    \"message\": \"{{name}}, Your {{entityType}} credential has been revoked\",    \"to\": [      \"{{contact}}\"    ]  }],\"DLT_TE_ID\": \"templateId\"}");
}
