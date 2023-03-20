package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dev.sunbirdrc.registry.model.Event;
import dev.sunbirdrc.registry.util.Definition;

import java.util.Date;
import java.util.UUID;

public interface IEventService {

    default Event createTelemetryObject(String eid,
                                        String actorId,
                                        String actorType,
                                        String objectId,
                                        String objectType,
                                        JsonNode edata
    ) {
        Event event = new Event();
        event.setEid(eid);
        event.setEts(new Date().getTime());
        event.setVer("3.1");
        event.setMid(UUID.randomUUID().toString());
        event.setActor(actorId, actorType);
        event.setObject(objectId, objectType);
        event.setEdata(edata);
        return event;
    }
    void pushEvents(Event event) throws JsonProcessingException;
}
