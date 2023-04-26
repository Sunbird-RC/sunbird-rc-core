package dev.sunbirdrc.registry.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NotificationTemplates {
    private NotificationTemplate createNotificationTemplates = new NotificationTemplate("Credential Created", "{{name}}, Your {{entityType}} credential has been created");
    private NotificationTemplate updateNotificationTemplates = new NotificationTemplate("Credential Updated", "{{name}}, Your {{entityType}} credential has been updated");
    private NotificationTemplate inviteNotificationTemplates = new NotificationTemplate("Invitation", "{{name}}, You have been invited");
    private NotificationTemplate deleteNotificationTemplates = new NotificationTemplate("Revoked", "{{name}}, Your {{entityType}} credential has been revoked");
}
