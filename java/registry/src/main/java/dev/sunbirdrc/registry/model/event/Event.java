package dev.sunbirdrc.registry.model.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;

@JsonSerialize
@Getter
public class Event {
    @JsonProperty("eid")
    private String eid;
    @JsonProperty("ets")
    private Long ets;
    @JsonProperty("ver")
    private String ver;
    @JsonProperty("mid")
    private String mid;
    @JsonProperty("actor")
    private Actor actor;
    @JsonProperty("object")
    private TelemetryObject object;
    @JsonProperty("edata")
    private JsonNode edata;

    public Event() {
        actor = new Actor();
        object = new TelemetryObject();
    }

    public void setEid(String eid) {
        this.eid = eid;
    }

    public void setEts(Long ets) {
        this.ets = ets;
    }

    public void setVer(String ver) {
        this.ver = ver;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public void setActor(String id, String type) {
        this.actor.setId(id);
        this.actor.setType(type);
    }

    public void setObject(String id, String type) {
        this.object.setId(id);
        this.object.setType(type);
    }

    public void setEdata(JsonNode edata) {
        this.edata = edata;
    }
}
