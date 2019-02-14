package io.opensaber.registry.transform;

public class ConfigurationHelper {
	public Configuration getResponseConfiguration(boolean requireLDResponse) {
		if (requireLDResponse) {
			return getJson2LDResponseConfiguration();
		} else {
			return getDefaultResponseConfiguration();
		}
	}

	public Configuration getDefaultResponseConfiguration() {
		return Configuration.JSON2JSON;
	}

	public Configuration getJson2LDResponseConfiguration() {
		return Configuration.JSON2LD;
	}
}
