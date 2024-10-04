package dev.sunbirdrc.registry.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.sunbird.akka.core.SunbirdActorFactory;

@Component
public class ActorInitializer {
    @PostConstruct
    public void init() {
        Config config = ConfigFactory.parseResources("sunbirdrc-actors.conf");
        SunbirdActorFactory sunbirdActorFactory = new SunbirdActorFactory(config, "dev.sunbirdrc.actors");
        sunbirdActorFactory.init("sunbirdrc-actors");

    }
}