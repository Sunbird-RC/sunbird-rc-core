package io.opensaber.registry.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EntityTypeHandler {
    private final List<String> internalEntities;
    private final List<String> externalEntities;

    public EntityTypeHandler(@Value("${registry.internalentities}") List<String> internalEntities, @Value("${registry.externalentities}")
            List<String> externalEntities) {
        this.internalEntities = internalEntities;
        this.externalEntities = externalEntities;
    }

    public boolean isInternalRegistry(String entityName) {
        return internalEntities.contains(entityName);
    }

    public boolean isExternalRegistry(String entityName) {
        return externalEntities.contains(entityName);
    }
}
