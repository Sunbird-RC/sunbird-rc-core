package dev.sunbirdrc.registry.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Actor {
    private final String id;
    private final String type;
}
