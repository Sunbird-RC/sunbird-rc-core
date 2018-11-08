package io.opensaber.registry.schema.configurator;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.opensaber.registry.middleware.util.Constants;

@Component
public class SchemaConfiguratorFactory {

	private Environment environment;

	private static Logger logger = LoggerFactory.getLogger(SchemaConfiguratorFactory.class);

	public SchemaConfiguratorFactory(Environment environment) {
		this.environment = environment;
	}

	public ISchemaConfigurator getInstance(SchemaType schemaType) {
		ISchemaConfigurator schemaConfigurator = null;
		try {
			String schemaFile = environment.getProperty(Constants.FIELD_CONFIG_SCEHEMA_FILE);
			if (schemaFile == null) {
				throw new IOException(Constants.SCHEMA_CONFIGURATION_MISSING);
			}
			switch (schemaType) {
			case JSON:
				schemaConfigurator = new JsonSchemaConfigurator(schemaFile);
				break;
			case SHEX:
				schemaConfigurator = new ShexSchemaConfigurator(schemaFile);
				break;
			}
		} catch (IOException ioe) {
			logger.error("Missing config schema file {%s}", ioe);
		}
		return schemaConfigurator;
	}

}
