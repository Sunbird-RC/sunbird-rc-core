package io.opensaber.registry.interceptor.request.transform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import io.opensaber.registry.middleware.transform.ErrorCode;
import io.opensaber.registry.middleware.transform.ITransformer;
import io.opensaber.registry.middleware.transform.TransformationException;

public class RequestTransformFactory {

	private static final String MEDIATYPE_APPLICATION_JSONLD = "application/ld+json";
	private static final String EXCEPTION_MESSAGE = "Media type not supported";

	@Autowired
	private JsonToLdRequestTransformer jsonToLdRequestTransformer;

	@Autowired
	private JsonldToLdRequestTransformer jsonldToLdRequestTransformer;

	public ITransformer<Object> getInstance(String type) throws TransformationException {
		ITransformer<Object> requestTransformer = null;

		switch (type.toLowerCase()) {

		case MEDIATYPE_APPLICATION_JSONLD:
			requestTransformer = jsonldToLdRequestTransformer;
			break;

		case MediaType.APPLICATION_JSON_VALUE:
			requestTransformer = jsonToLdRequestTransformer;
			break;

		case MediaType.ALL_VALUE:
			requestTransformer = jsonToLdRequestTransformer;
			break;

		default:
			throw new TransformationException(EXCEPTION_MESSAGE, ErrorCode.UNSUPPOTERTED_TYPE);

		}
		return requestTransformer;
	}

}
