package dev.sunbirdrc.pojos;

import lombok.ToString;

import java.util.Map;

@ToString
public class OPADataResponse {

    private Map<String, Object> result;

    public OPADataResponse() {

    }

    public Map<String, Object> getResult() {
        return this.result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }

}