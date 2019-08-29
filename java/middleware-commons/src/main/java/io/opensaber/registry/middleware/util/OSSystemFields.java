package io.opensaber.registry.middleware.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.EnumSet;

/**
 * System fields for created, updated time and userId appended to the json node.
 */
public enum OSSystemFields {

    osCreatedAt {
        @Override
        public void createdAt(JsonNode node, String timeStamp) {
            JSONUtil.addField((ObjectNode) node, osCreatedAt.toString(), timeStamp);
        }
    },
    osUpdatedAt {
        @Override
        public void updatedAt(JsonNode node, String timeStamp) {
            JSONUtil.addField((ObjectNode) node, osUpdatedAt.toString(), timeStamp);
        }

    },
    osCreatedBy {
        @Override
        public void createdBy(JsonNode node, String userId) {
            JSONUtil.addField((ObjectNode) node, osCreatedBy.toString(), userId != null ? userId : "");
        }
    },
    osUpdatedBy {
        @Override
        public void updatedBy(JsonNode node, String userId) {
            JSONUtil.addField((ObjectNode) node, osUpdatedBy.toString(), userId != null ? userId : "");
        }
    };

    public void createdBy(JsonNode node, String userId){};

    public void updatedBy(JsonNode node, String userId){};

    public void createdAt(JsonNode node, String timeStamp){};

    public void updatedAt(JsonNode node, String timeStamp){};

    public static OSSystemFields getByValue(String value) {
        for (final OSSystemFields element : EnumSet.allOf(OSSystemFields.class)) {
            if (element.toString().equals(value)) {
                return element;
            }
        }
        return null;
    }

}
