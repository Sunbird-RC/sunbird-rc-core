package io.opensaber.validators.json;

import java.io.IOException;

import io.opensaber.pojos.ValidationResponse;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.validators.core.ValidationService;
import io.opensaber.validators.exception.RDFValidationException;

public class JsonValidatorServiceImpl implements ValidationService {

	@Override
	public ValidationResponse validateData(Object rdfModel, String origin)
			throws RDFValidationException, MiddlewareHaltException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
