package io.opensaber.registry.transform;

import io.opensaber.registry.middleware.util.Constants.Direction;
import org.springframework.http.MediaType;

public class ConfigurationHelper {

	public static final String MEDIATYPE_APPLICATION_JSONLD = "application/ld+json";

	public Configuration getConfiguration(String mediaType, Direction direction) {
		// Note: LD2LD is treated as default configuration which basically by
		// pass the content.
		if (mediaType.equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE) && direction == Direction.OUT) {
			return Configuration.LD2LD;
		} else if (mediaType.equalsIgnoreCase(MEDIATYPE_APPLICATION_JSONLD) && direction == Direction.OUT) {
			return Configuration.JSON2LD;
		} else if (mediaType.equalsIgnoreCase(MediaType.ALL_VALUE) && direction == Direction.OUT) {
			return Configuration.LD2LD;
		}
		return null;
	}
}
