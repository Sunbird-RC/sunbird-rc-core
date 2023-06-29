package dev.sunbirdrc.registry.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationTemplate {
    private String subject;
    private String body;
}
