package io.opensaber.registry.middleware.util;

import com.fasterxml.jackson.databind.JsonNode;

public class EntityUtil {
    public static String getFullNameOfTheEntity(JsonNode entityNode) {
        if (entityNode.hasNonNull("identityDetails") && entityNode.get("identityDetails").has("fullName")) {
            return entityNode.get("identityDetails")
                    .get("fullName")
                    .asText();
        } else {
            return "";
        }
    }
}
