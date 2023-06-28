package dev.sunbirdrc.elastic;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public class ESMessage {
    String indexName;
    String osid;


    public ESMessage() {
    }

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
}







