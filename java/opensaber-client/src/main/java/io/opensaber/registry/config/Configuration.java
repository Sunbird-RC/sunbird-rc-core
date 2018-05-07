package io.opensaber.registry.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Configuration {

    private final static String environment =
            System.getenv("OPENSABER_CLIENT_ENVIRONMENT") == null
                    ? "dev"
                    : System.getenv("OPENSABER_CLIENT_ENVIRONMENT");

    private static Configuration instance = new Configuration();
    private Config config;

    public static Configuration getInstance() {
        return instance;
    }

    public Config getConfig() {
        return config;
    }

    private Configuration() {
        this.config = load();
    }

    private Config load() {
        Config config = ConfigFactory.load().getConfig("opensaber-client");
        if (config.hasPath(environment)) {
            return config.getConfig(environment).withFallback(config);
        }
        return config;
    }
}
