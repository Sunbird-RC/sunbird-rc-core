package dev.sunbirdrc.registry.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

public class Event {
    private String eid;
    private Long ets;
    private String ver;
    private String mid;
    private Map<String, String> actor;
    private Map<String, String> object;
    private Map<String, Object> edata;

    public Event() {
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
        Map<String, String> map = new HashMap<>();
        map.put("id", id);
        map.put("type", type);
        this.actor = map;
    }

    public void setObject(String id, String type) {
        Map<String, String> map = new HashMap<>();
        map.put("id", id);
        map.put("type", type);
        this.object = map;
    }

    public void setEdata(JsonNode edata) {
        ObjectMapper objectMapper = new ObjectMapper();
        this.edata = objectMapper.convertValue(edata, new TypeReference<Map<String, Object>>() {});
    }

    @Override
    public String toString() {
        return "Event{" +
                "eid='" + eid + '\'' +
                ", ets=" + ets +
                ", ver='" + ver + '\'' +
                ", mid='" + mid + '\'' +
                ", actor=" + actor +
                ", object=" + object +
                ", edata=" + edata +
                '}';
    }

    public String getObjectId() {
        return this.object.get("id");
    }
}
