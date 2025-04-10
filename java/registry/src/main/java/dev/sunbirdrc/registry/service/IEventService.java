package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dev.sunbirdrc.registry.model.event.Actor;
import dev.sunbirdrc.registry.model.event.Event;
import dev.sunbirdrc.registry.model.event.TelemetryObject;

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
        Event event = Event.builder()
                .object(new TelemetryObject(objectId, objectType))
                .actor(new Actor(actorId, actorType))
                .ets(new Date().getTime())
                .mid(UUID.randomUUID().toString())
                .ver("3.1")
                .eid(eid)
                .edata(edata)
                .build();
        return event;
    }

    void pushEvents(Event event) throws JsonProcessingException;
}
