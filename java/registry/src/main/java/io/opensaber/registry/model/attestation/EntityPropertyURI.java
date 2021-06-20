package io.opensaber.registry.model.attestation;

import com.fasterxml.jackson.core.JsonPointer;

public class EntityPropertyURI {
    String propertyURI;
    JsonPointer jsonPointer;

    private EntityPropertyURI() { }

    public EntityPropertyURI(String propertyURI, String jsonPointer) {
        this.jsonPointer = JsonPointer.compile(jsonPointer);
        this.propertyURI = propertyURI;
    }

    public JsonPointer getJsonPointer() {
        return jsonPointer;
    }

    public String getPropertyURI() {
        return propertyURI;
    }

    public static EntityPropertyURI merge(EntityPropertyURI m1, String uuidPath, String jsonPath) {
        EntityPropertyURI merged = new EntityPropertyURI();
        merged.propertyURI = m1.propertyURI + uuidPath;
        merged.jsonPointer = JsonPointer.compile(m1.jsonPointer.toString() + jsonPath);
        return merged;
    }
}
