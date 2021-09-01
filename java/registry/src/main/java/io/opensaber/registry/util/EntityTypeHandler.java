package io.opensaber.registry.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class EntityTypeHandler {
    private final List<String> internalEntities;
    private final List<String> externalEntities;

    public EntityTypeHandler(@Value("${registry.internalentities}") String[] internalEntities, @Value("${registry.externalentities}") String[] externalEntities) {
        this.internalEntities = Arrays.asList(internalEntities);
        this.externalEntities = Arrays.asList(externalEntities);
    }

    public boolean isInternalRegistry(String entityName) {
        return internalEntities.contains(entityName);
    }

    public boolean isExternalRegistry(String entityName) {
        return externalEntities.contains(entityName);
    }
}
