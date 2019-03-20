package io.opensaber.elastic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public class ESMessage {
    String indexName;
    String osid;
    JsonNode input;

    public ESMessage() {
    }

    /*public ESMessage(String indexName, String osid, JsonNode input) {
        setIndexName(indexName);
        setOsid(osid);
        setInput(input);
    }*/

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getOsid() {
        return osid;
    }

    public void setOsid(String osid) {
        this.osid = osid;
    }

    public JsonNode getInput() {
        return input;
    }

    public void setInput(JsonNode input) {
        this.input = input;
    }
}
