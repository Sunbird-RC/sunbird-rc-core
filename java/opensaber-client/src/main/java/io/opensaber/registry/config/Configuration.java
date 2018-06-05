package io.opensaber.registry.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Configuration {

    private final static String environment =
            System.getenv("opensaber_client_environment") == null
                    ? "dev"
                    : System.getenv("opensaber_client_environment");

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

    public static final String MAPPING_FILE = Configuration.instance.config.getString("mapping.file");

    public static final String HOST = Configuration.instance.config.getString("registry.service.host");
    public static final Integer PORT = Configuration.instance.config.getInt("registry.service.port");
    public static final String BASE_URL = Configuration.instance.config.getString("registry.service.baseUrl");
    public static final Integer HTTP_CONNECT_TIMEOUT = Configuration.instance.config.getInt("registry.http.connect-timeout");
    public static final Integer HTTP_READ_TIMEOUT = Configuration.instance.config.getInt("registry.http.read-timeout");
    public static final Integer HTTP_CONNECTION_REQUEST_TIMEOUT = Configuration.instance.config.getInt("registry.http.connection-request-timeout");
}
