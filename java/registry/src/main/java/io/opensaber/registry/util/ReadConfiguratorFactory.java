package io.opensaber.registry.util;

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

    public static ReadConfigurator getForUpdateValidation() {
        ReadConfigurator configurator = new ReadConfigurator();
        // For update, there could be signatures required.
        configurator.setIncludeSignatures(true);

        // Get rid of type attributes, which would fail validation
        configurator.setIncludeTypeAttributes(true);

        // Load uuidPropertyNames too and remove before validation
        configurator.setIncludeIdentifiers(true);

        // We want to know if the passed in value is child or not
        configurator.setIncludeRootIdentifiers(true);

        return configurator;
    }
}
