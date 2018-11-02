package io.opensaber.validators.core;

import java.io.IOException;

import io.opensaber.pojos.ValidationResponse;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.validators.exception.RDFValidationException;

public interface ValidationService {

    public ValidationResponse validateData(Object rdfModel, String origin) throws RDFValidationException,MiddlewareHaltException,IOException;
}
