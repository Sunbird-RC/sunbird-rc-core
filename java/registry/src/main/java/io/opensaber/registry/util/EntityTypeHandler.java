package io.opensaber.registry.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class EntityTypeHandler {
    private final List<String> externalEntities;

    public EntityTypeHandler(@Value("${registry.externalentities}") String[] externalEntities) {
        this.externalEntities = Arrays.asList(externalEntities);
    }

    public boolean isExternalRegistry(String entityName) {
        return externalEntities.contains(entityName);
    }
}
