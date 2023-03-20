package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.registry.model.Event;
import dev.sunbirdrc.registry.service.IEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventService implements IEventService {

    @Value("${kafka.metricsTopic:metrics}")
    String metricsTopic;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Override
    public void pushEvents(Event event) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String message = objectMapper.writer().writeValueAsString(event);
        kafkaTemplate.send(metricsTopic, event.getObjectId(), message);
    }
}
