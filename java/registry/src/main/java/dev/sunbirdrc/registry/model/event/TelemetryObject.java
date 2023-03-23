package dev.sunbirdrc.registry.model.event;

import lombok.Data;

@Data
public class TelemetryObject {
    String id;
    String type;

    public String getId() {
        return id;
    }
}
