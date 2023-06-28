package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import dev.sunbirdrc.registry.model.event.Event;
import dev.sunbirdrc.registry.service.IEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "event.providerName", havingValue = "dev.sunbirdrc.registry.service.impl.KafkaEventService", matchIfMissing = true)
public class KafkaEventService implements IEventService {

    @Value("${events.topic:events}")
    String metricsTopic;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Override
    public void pushEvents(Event event) throws JsonProcessingException {
        ObjectWriter objectMapper = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String message = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(metricsTopic, event.getObject().getId(), message);
    }
}
