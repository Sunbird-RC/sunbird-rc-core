package io.opensaber.registry.transform.utils;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonUtils {

    public static ObjectNode createObjectNode() {
        return JsonNodeFactory.instance.objectNode();
    }
}
