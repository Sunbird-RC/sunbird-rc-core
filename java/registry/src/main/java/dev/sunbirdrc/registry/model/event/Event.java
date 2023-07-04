package dev.sunbirdrc.registry.model.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;

@JsonSerialize
@Builder
public class Event {
    @JsonProperty("eid")
    private final String eid;
    @JsonProperty("ets")
    private final Long ets;
    @JsonProperty("ver")
    private final String ver;
    @JsonProperty("mid")
    private final String mid;
    @JsonProperty("actor")
    private final Actor actor;
    @JsonProperty("object")
    private final TelemetryObject object;
    @JsonProperty("edata")
    private final JsonNode edata;

    public TelemetryObject getObject() {
        return object;
    }
}