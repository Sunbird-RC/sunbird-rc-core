package dev.sunbirdrc.plugin.components;

import dev.sunbirdrc.plugin.services.MosipServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("#{(environment.MOSIP_ENABLED?:'false').equals('true')}")
public class MosipInit {
    public static final Logger logger = LoggerFactory.getLogger(MosipInit.class);
    @Autowired
    MosipServices mosipServices;

    @EventListener
    public void initSubscriptions(final ApplicationReadyEvent event) {
        mosipServices.initSubscriptions();
    }
}
