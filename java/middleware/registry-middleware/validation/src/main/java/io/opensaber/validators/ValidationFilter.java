package io.opensaber.validators;

import java.io.IOException;
import java.util.Map;

import org.apache.jena.rdf.model.Model;

import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;

public class ValidationFilter implements Middleware {
	private IValidate validationService;

	public ValidationFilter(IValidate validationServiceImpl) {
		this.validationService = validationServiceImpl;
	}

	@Override
	public Map<String, Object> execute(Map<String, Object> mapData) throws IOException, MiddlewareHaltException {
		Model rdfModel = (Model) mapData.get(Constants.RDF_OBJECT);
		String method = mapData.get(Constants.METHOD_ORIGIN).toString().replace("/", "");

		if (null == rdfModel) {
			// json based validation likely
			validationService.validate(mapData.get(Constants.ATTRIBUTE_NAME), method);
		} else {
			validationService.validate(mapData.get("rdf"), method);
		}
		return mapData;
	}

	@Override
	public Map<String, Object> next(Map<String, Object> mapData) throws IOException {
		return null;
	}
}
