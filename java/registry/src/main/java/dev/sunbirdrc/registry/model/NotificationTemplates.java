package dev.sunbirdrc.registry.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NotificationTemplates {
    private List<NotificationTemplate> create;
    private List<NotificationTemplate> update;
    private List<NotificationTemplate> invite;
    private List<NotificationTemplate> delete;
    private List<NotificationTemplate> revoke;

    public NotificationTemplates() {
        create = new ArrayList<>();
        update = new ArrayList<>();
        invite = new ArrayList<>();
        delete = new ArrayList<>();
        revoke = new ArrayList<>();
    }
}