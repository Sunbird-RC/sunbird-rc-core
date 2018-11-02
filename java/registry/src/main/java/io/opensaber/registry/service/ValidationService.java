package io.opensaber.registry.service;

import io.opensaber.pojos.ValidationResponse;
import io.opensaber.registry.exception.RDFValidationException;
import io.opensaber.registry.middleware.MiddlewareHaltException;

import java.io.IOException;

public interface ValidationService {

    public ValidationResponse validateData(Object rdfModel, String origin) throws RDFValidationException, MiddlewareHaltException, IOException;
}
