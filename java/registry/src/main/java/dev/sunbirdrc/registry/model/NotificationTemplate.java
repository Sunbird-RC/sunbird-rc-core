package dev.sunbirdrc.registry.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationTemplate {
    private String subject;
    private String body;
}
