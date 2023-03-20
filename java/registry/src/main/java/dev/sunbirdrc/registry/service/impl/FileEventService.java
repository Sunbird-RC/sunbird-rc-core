package dev.sunbirdrc.registry.service.impl;

import dev.sunbirdrc.registry.model.Event;
import dev.sunbirdrc.registry.service.IEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FileEventService implements IEventService {

    private static Logger logger = LoggerFactory.getLogger(FileEventService.class);
    @Override
    public void pushEvents(Event event) {
        logger.info("{}", event.toString());
    }
}
