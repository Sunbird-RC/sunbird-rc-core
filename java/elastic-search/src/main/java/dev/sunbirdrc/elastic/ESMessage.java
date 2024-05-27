package dev.sunbirdrc.elastic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public class ESMessage {
    String indexName;
    String uuidPropertyValue;
    JsonNode input;

    public ESMessage() {
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getUuidPropertyValue() {
        return uuidPropertyValue;
    }

    public void setUuidPropertyValue(String uuidPropertyValue) {
        this.uuidPropertyValue = uuidPropertyValue;
    }

    public JsonNode getInput() {
        return input;
    }

    public void setInput(JsonNode input) {
        this.input = input;
    }
}
