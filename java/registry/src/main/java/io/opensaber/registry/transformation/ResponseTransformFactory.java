package io.opensaber.registry.transformation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import io.opensaber.registry.middleware.transform.ErrorCode;
import io.opensaber.registry.middleware.transform.ITransformer;
import io.opensaber.registry.middleware.transform.TransformationException;
import io.opensaber.registry.transformation.JsonToLdTransformer;
import io.opensaber.registry.transformation.JsonldToLdTransformer;

@Component
public class ResponseTransformFactory {

	private static final String MEDIATYPE_APPLICATION_JSONLD = "application/ld+json";
	private static final String EXCEPTION_MESSAGE = "Media type not suppoted";

	@Autowired
	private JsonToLdTransformer jsonTransformer;

	@Autowired
	private JsonldToLdTransformer jsonldTransformer;

	public ITransformer<Object> getInstance(MediaType type) throws TransformationException {
		ITransformer<Object> responseTransformer = null;

		switch (type.toString()) {

		case MEDIATYPE_APPLICATION_JSONLD:
			responseTransformer = jsonldTransformer;
			break;

		case MediaType.APPLICATION_JSON_VALUE:
			responseTransformer = jsonTransformer;
			break;
			
		case MediaType.ALL_VALUE:
			responseTransformer = jsonTransformer;
			break;	

		default:
			throw new TransformationException(EXCEPTION_MESSAGE, ErrorCode.UNSUPPOTERTED_TYPE);

		}
		return responseTransformer;
	}

}
