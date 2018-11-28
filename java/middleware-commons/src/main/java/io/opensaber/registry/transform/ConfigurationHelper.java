package io.opensaber.registry.transform;

import org.springframework.http.MediaType;

import io.opensaber.registry.middleware.util.Constants.Direction;

public class ConfigurationHelper {
	
	public static final String MEDIATYPE_APPLICATION_JSONLD = "application/ld+json";

	public Configuration getConfiguration(String mediaType, Direction direction){
		
		if(mediaType.equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE) && direction == Direction.OUT){
			return Configuration.LD2JSON;
		}else if(mediaType.equalsIgnoreCase(MEDIATYPE_APPLICATION_JSONLD)){
			return Configuration.LD2LD;
		}else if(mediaType.equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE) && direction == Direction.IN){
			return Configuration.JSON2LD;
		}		
		return null;
	}
}
