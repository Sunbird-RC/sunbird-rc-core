package io.opensaber.registry.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class ReadConfiguratorFactory {
    public static ReadConfigurator getDefault() {
        ReadConfigurator configurator = new ReadConfigurator();
        configurator.setIncludeSignatures(false);
        configurator.setIncludeTypeAttributes(false);
        return configurator;
    }

    public static ReadConfigurator getWithSignatures() {
        ReadConfigurator configurator = new ReadConfigurator();
        configurator.setIncludeSignatures(true);
        configurator.setIncludeTypeAttributes(false);
        return configurator;
    }

    public static ReadConfigurator getOne(boolean withSignatures) {
        if (withSignatures) {
            return getWithSignatures();
        } else {
            return getDefault();
        }
    }

    public static ReadConfigurator getOne(boolean withSignatures, JsonNode configRequested) {
        ReadConfigurator readConfigurator = null;
        if (configRequested != null && !configRequested.isNull()) {
            try {
                readConfigurator = new ObjectMapper().readValue(configRequested.toString(),
                        ReadConfigurator.class);
            } catch (IOException e) {
                // No explicit config requested. Not a problem.
                readConfigurator = null;
            }
        }

        if (null == readConfigurator) {
            readConfigurator = getOne(withSignatures);
        }
        return readConfigurator;
    }

    public static ReadConfigurator getForUpdateValidation() {
        ReadConfigurator configurator = new ReadConfigurator();
        // For update, there could be signatures required.
        configurator.setIncludeSignatures(true);

        // Get rid of type attributes, which would fail validation
        configurator.setIncludeTypeAttributes(true);

        // Load uuidPropertyNames too and remove before validation
        configurator.setIncludeIdentifiers(true);

        // We want to know if the passed in value is child or not
        configurator.setIncludeRootIdentifiers(false);

        return configurator;
    }
}
