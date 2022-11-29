package dev.sunbirdrc.validators;

import com.fasterxml.jackson.databind.JsonNode;
import dev.sunbirdrc.registry.middleware.MiddlewareHaltException;

import java.io.IOException;

public interface IValidate {

    void validate(String entityType, String payload, boolean ignoreRequiredFields) throws MiddlewareHaltException;

    /**
     * Store all list of known definitions as definitionMap.
     * Must get populated before creating the schema.
     *
     * @param definitionTitle
     * @param definitionContent
     */
    void addDefinitions(String definitionTitle, String definitionContent);

    void addDefinitions(JsonNode schema) throws IOException;

    void removeDefinition(JsonNode jsonNode);
}
