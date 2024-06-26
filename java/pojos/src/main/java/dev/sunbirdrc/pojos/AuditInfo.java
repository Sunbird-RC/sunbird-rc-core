package dev.sunbirdrc.pojos;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AuditInfo {
    private String op;
    private String path;
    private Object value;
    private String from;

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @JsonIgnore
    public String getFrom() {
        return from;
    }

    public void setFrom(String path) {
        this.from = path;
    }

    @JsonIgnore
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

}
