package dev.sunbirdrc.registry.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TelemetryObject {
    private final String id;
    private final String type;
}