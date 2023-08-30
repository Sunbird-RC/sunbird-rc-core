package dev.sunbirdrc.claim.controller;

import dev.sunbirdrc.claim.entity.Event;
import dev.sunbirdrc.claim.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1/api/events")
public class EventController
{
    @Autowired
    private EventService eventService;


    @Autowired
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<Event> createEventWithTelemetry(@RequestBody Event event) {
        Event savedEvent = eventService.saveEventWithTelemetry(event);
        return ResponseEntity.ok(savedEvent);
    }

    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        List<Event> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }
}
