package io.opensaber.validators;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.registry.middleware.MiddlewareHaltException;

public interface IValidate {

    boolean validate(String entityType, String payload) throws MiddlewareHaltException;
    String getEntitySubject(String entityType, JsonNode entity) throws Exception;

    /**
     * Store all list of known definitions as definitionMap.
     * Must get populated before creating the schema.
     * 
     * @param definitionTitle
     * @param definitionContent
     */
    void addDefinitions(String definitionTitle, String definitionContent);
}
