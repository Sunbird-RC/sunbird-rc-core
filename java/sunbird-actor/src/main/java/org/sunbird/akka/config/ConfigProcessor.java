package org.sunbird.akka.config;

import com.typesafe.config.Config;

/**
 * Defines the configuration format expected by this library
 */
public class ConfigProcessor {
    private Config config;

    public ConfigProcessor(Config config) {
        this.config = config;
    }

    public Config getConfig() {
        return config;
    }
}
