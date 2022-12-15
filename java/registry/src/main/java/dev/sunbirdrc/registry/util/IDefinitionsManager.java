package dev.sunbirdrc.registry.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.pojos.OwnershipsAttributes;
import dev.sunbirdrc.registry.middleware.util.Constants;
import org.springframework.core.io.ResourceLoader;

import java.util.*;
import java.util.stream.Collectors;

import static dev.sunbirdrc.registry.Constants.USER_ANONYMOUS;

public interface IDefinitionsManager {
    void loadDefinition() throws Exception;

    Set<String> getAllKnownDefinitions();
    List<Definition> getAllDefinitions();
    Definition getDefinition(String title);

    Map<String, Definition> getDefinitionMap();

    void setInternalSchemas() throws Exception;
    List<String> getInternalSchemas();


    /**
     * Returns the map, where key is the index and value is the public fields
     *
     * @return
     */
    default Map<String, Set<String>> getPublicFieldsInfoMap() {
        Map<String, Set<String>> result = new HashMap<>();
        for (String index : getAllKnownDefinitions()) {
            List<String> publicFields = getDefinition(index)
                    .getOsSchemaConfiguration()
                    .getPublicFields();
            if (publicFields != null) {
                result.put(index.toLowerCase(), new HashSet<>(publicFields));
            } else {
                result.put(index.toLowerCase(), Collections.emptySet());
            }
        }
        return result;
    }

    /**
     * Returns the map, where key is the index and value is the internal fields
     *
     * @return
     */
    default Map<String, Set<String>> getExcludingFields() {
        Map<String, Set<String>> result = new HashMap<>();
        for (String index : getAllKnownDefinitions()) {
            List<String> internalFields = getDefinition(index)
                    .getOsSchemaConfiguration()
                    .getInternalFields();
            List<String> privateFields = getDefinition(index)
                    .getOsSchemaConfiguration()
                    .getPrivateFields();
            internalFields.addAll(privateFields);
            result.put(index.toLowerCase(), new HashSet<>(internalFields));
        }
        return result;
    }

    default Set<String> getExcludingFieldsForEntity(String entity) {
        Definition definition = getDefinition(entity);
        Set<String> excludeFields = new HashSet<>();
        List<String> internalFields = definition
                .getOsSchemaConfiguration()
                .getInternalFields();
        List<String> privateFields = definition
                .getOsSchemaConfiguration()
                .getPrivateFields();
        excludeFields.addAll(internalFields);
        excludeFields.addAll(privateFields);
        return excludeFields;
    }
    List<OwnershipsAttributes> getOwnershipAttributes(String entity);
    default Object getCredentialTemplate(String entityName) {
        return getDefinition(entityName).getOsSchemaConfiguration().getCredentialTemplate();
    }
    default Map<String, String> getCertificateTemplates(String entityName) {
        return getDefinition(entityName).getOsSchemaConfiguration().getCertificateTemplates();
    }
    boolean isValidEntityName(String entityName);
    void appendNewDefinition(JsonNode jsonNode);
    void appendNewDefinition(Definition definition);
    void removeDefinition(JsonNode jsonNode);

    default List<String> getEntitiesWithAnonymousInviteRoles() {
        List<String> anonymousEntities = new ArrayList<>();
        for (Map.Entry<String, Definition> definitionEntry : getDefinitionMap().entrySet()) {
            Definition definition = definitionEntry.getValue();
            if (definition.getOsSchemaConfiguration().getInviteRoles().contains(USER_ANONYMOUS)) {
                anonymousEntities.add(definitionEntry.getKey());
            }
        }
        return anonymousEntities;
    }

    default List<String> getEntitiesWithAnonymousManageRoles() {
        List<String> anonymousEntities = new ArrayList<>();
        for (Map.Entry<String, Definition> definitionEntry : getDefinitionMap().entrySet()) {
            Definition definition = definitionEntry.getValue();
            if (definition.getOsSchemaConfiguration().getRoles().contains(USER_ANONYMOUS)) {
                anonymousEntities.add(definitionEntry.getKey());
            }
        }
        return anonymousEntities;
    }

    default List<String> getInternalSchemasNames(ResourceLoader resourceLoader) throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        OSResourceLoader osResourceLoader = new OSResourceLoader(resourceLoader);
        osResourceLoader.loadResource(Constants.INTERNAL_RESOURCE_LOCATION);
        return osResourceLoader.getNameContent().values().stream().map(s -> {
            try {
                JsonNode jsonNode = mapper.readTree(s);
                Definition definition = new Definition(jsonNode);
                return definition.getTitle();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

        }).collect(Collectors.toList());

    }
}
