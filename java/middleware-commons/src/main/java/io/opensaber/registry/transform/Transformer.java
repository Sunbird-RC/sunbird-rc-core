package io.opensaber.registry.transform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import io.opensaber.registry.middleware.util.Constants.Direction;

public class Transformer {

	public static final String MEDIATYPE_APPLICATION_JSONLD = "application/ld+json";
	private static final String EXCEPTION_MESSAGE = "Media type not suppoted";

	@Autowired
	private Json2LdTransformer json2LdTransformer;

	@Autowired
	private Ld2JsonTransformer ld2JsonTransformer;
	
	@Autowired
	private Ld2LdTransformer Ld2LdTransformer;
	
	public ITransformer<Object> getInstance(Configuration config) throws TransformationException {
		ITransformer<Object> transformer = null;
		
		if(config == Configuration.JSON2LD){
			transformer = json2LdTransformer;
		}else if(config == Configuration.LD2JSON){
			transformer = ld2JsonTransformer;
		}else if(config == Configuration.LD2LD){
			transformer = Ld2LdTransformer;
		}else{
			throw new TransformationException(EXCEPTION_MESSAGE, ErrorCode.UNSUPPOTERTED_TYPE);

		}			
		return transformer;
	}
	
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
