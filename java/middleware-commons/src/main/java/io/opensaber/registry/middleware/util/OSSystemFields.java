package io.opensaber.registry.middleware.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.EnumSet;

/**
 * System fields for created, updated time and userId appended to the json node.
 */
public enum OSSystemFields {

    _osCreatedAt {
        @Override
        public void createdAt(JsonNode node, String timeStamp) {
            JSONUtil.addField((ObjectNode) node, _osCreatedAt.toString(), timeStamp);
        }
    },
    _osUpdatedAt {
        @Override
        public void updatedAt(JsonNode node, String timeStamp) {
            JSONUtil.addField((ObjectNode) node, _osUpdatedAt.toString(), timeStamp);
        }

    },
    _osCreatedBy {
        @Override
        public void createdBy(JsonNode node, String userId) {
            JSONUtil.addField((ObjectNode) node, _osCreatedBy.toString(), userId != null ? userId : "");
        }
    },
    _osUpdatedBy {
        @Override
        public void updatedBy(JsonNode node, String userId) {
            JSONUtil.addField((ObjectNode) node, _osUpdatedBy.toString(), userId != null ? userId : "");
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
