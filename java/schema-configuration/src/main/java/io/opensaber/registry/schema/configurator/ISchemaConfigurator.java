package io.opensaber.registry.schema.configurator;

import java.util.List;

public interface ISchemaConfigurator {
	
	public boolean isPrivate(String propertyName);
	public boolean isEncrypted(String tailPropertyKey);
	public List<String> getAllPrivateProperties();
	public String getSchemaContent();
	
}
