package dev.sunbirdrc.registry.util;

import com.fasterxml.jackson.databind.JsonNode;
import dev.sunbirdrc.pojos.OwnershipsAttributes;

import java.util.*;

public interface IDefinitionsManager {
    void loadDefinition() throws Exception;

    Set<String> getAllKnownDefinitions();
    List<Definition> getAllDefinitions();
    Definition getDefinition(String title);

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
    List<OwnershipsAttributes> getOwnershipAttributes(String entity);
    default Object getCredentialTemplate(String entityName) {
        return getDefinition(entityName).getOsSchemaConfiguration().getCredentialTemplate();
    }
    default Map<String, String> getCertificateTemplates(String entityName) {
        return getDefinition(entityName).getOsSchemaConfiguration().getCertificateTemplates();
    }
    boolean isValidEntityName(String entityName);
    void appendNewDefinition(JsonNode jsonNode);
    void removeDefinition(JsonNode jsonNode);
}
