package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import dev.sunbirdrc.registry.model.event.Event;
import dev.sunbirdrc.registry.service.IEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "event.providerName", havingValue = "dev.sunbirdrc.registry.service.impl.FileEventService")
public class FileEventService implements IEventService {

    private static Logger logger = LoggerFactory.getLogger(FileEventService.class);
    @Override
    public void pushEvents(Event event) throws JsonProcessingException {
        ObjectWriter objectWriter = new ObjectMapper().writer();
        String message = objectWriter.writeValueAsString(event);
        logger.info("{}", message);
    }
}
