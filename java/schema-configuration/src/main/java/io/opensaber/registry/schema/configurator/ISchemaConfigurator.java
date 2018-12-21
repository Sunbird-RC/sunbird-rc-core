package io.opensaber.registry.schema.configurator;

import java.util.List;

public interface ISchemaConfigurator {

	boolean isPrivate(String propertyName);

	boolean isEncrypted(String tailPropertyKey);

	List<String> getAllPrivateProperties();

	String getSchemaContent();

}
