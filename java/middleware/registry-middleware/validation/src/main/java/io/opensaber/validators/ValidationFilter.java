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
		validationService.validate(mapData.get("rdf"),
				mapData.get(Constants.METHOD_ORIGIN).toString().replace("/", ""));
		return mapData;
	}

	@Override
	public Map<String, Object> next(Map<String, Object> mapData) throws IOException {
		return null;
	}
}
