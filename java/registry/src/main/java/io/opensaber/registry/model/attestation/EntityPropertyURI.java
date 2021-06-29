package io.opensaber.registry.model.attestation;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Optional;

public class EntityPropertyURI {
    static final String NO_UUID = "NO_UUID";
    private String propertyURI;
    private JsonPointer jsonPointer;

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

    public static Optional<EntityPropertyURI> fromEntityAndPropertyURI(JsonNode node, String propertyURI, String uuidPropertyName) {
        String[] steps = (propertyURI.startsWith("/") ? propertyURI.substring(1) : propertyURI).split("/");
        JsonNode curr = node;
        for (int i = 0; i < steps.length; i++) {
            if (curr == null || curr.isMissingNode()) {
                return Optional.empty();
            }
            if (!curr.isArray()) {
                curr = curr.get(steps[i]);
                continue;
            }
            int index = -1;
            try {
                index = Integer.parseInt(steps[i]);
            } catch (NumberFormatException nfe) {
                if (steps[i].equals(EntityPropertyURI.NO_UUID)) {
                    return Optional.empty();
                }
                ArrayNode arrNode = (ArrayNode)curr;
                for (int j = 0; j < arrNode.size(); j++) {
                    if (arrNode.get(j).get(uuidPropertyName).asText().equals(steps[i])) {
                        steps[i] = String.valueOf(j);
                        index = j;
                    }
                }
            }
            if (index == -1) {
                return Optional.empty();
            }
            curr = curr.get(index);
        }
        return curr== null || curr.isMissingNode() ? Optional.empty() : Optional.of(new EntityPropertyURI(
                propertyURI,
                "/" + String.join("/", steps)
        ));
    }
}
