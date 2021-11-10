package io.opensaber.registry.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.pojos.OwnershipsAttributes;
import io.opensaber.pojos.attestation.AttestationPolicy;
import io.opensaber.registry.middleware.util.Constants;

import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.PostConstruct;

import org.apache.commons.collections.map.HashedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component("definitionsManager")
public class DefinitionsManager {
    private static Logger logger = LoggerFactory.getLogger(DefinitionsManager.class);

    private Map<String, Definition> definitionMap = new HashMap<>();
    private Map<String, Definition> derivedDefinitionMap = new HashedMap();

    private OSResourceLoader osResourceLoader;
    
    @Autowired
    private ResourceLoader resourceLoader;

    /**
     * Loads the definitions from the _schemas folder
     */
    @PostConstruct
    public void loadDefinition() throws Exception {

        final ObjectMapper mapper = new ObjectMapper();
        osResourceLoader = new OSResourceLoader(resourceLoader);
        osResourceLoader.loadResource(Constants.RESOURCE_LOCATION);

        for(Entry<String, String> entry : osResourceLoader.getNameContent().entrySet()) {
            String filename = entry.getKey();
            String filenameWithoutExtn = filename.substring(0, filename.indexOf('.'));
            JsonNode jsonNode = mapper.readTree(entry.getValue());
            Definition definition = new Definition(jsonNode);
            logger.info("loading resource:" + entry.getKey() + " with private field size:"
                    + definition.getOsSchemaConfiguration().getPrivateFields().size() + " & signed fields size:"
                    + definition.getOsSchemaConfiguration().getSignedFields().size());
            definitionMap.putIfAbsent(definition.getTitle(), definition);
            definitionMap.putIfAbsent(filenameWithoutExtn, definition);
        }

        derivedDefinitionMap.putAll(definitionMap);
        Set<Definition> loadedDefinitionsSet = new HashSet<>();
        loadedDefinitionsSet.addAll(definitionMap.values());

        // CAVEAT: attribute names must be distinct to not cause definition collisions.
        loadedDefinitionsSet.forEach(def -> {
            def.getSubSchemaNames().forEach((fieldName, defnName) -> {
                Definition definition = definitionMap.getOrDefault(defnName, null);
                if (null != definition) {
                    derivedDefinitionMap.putIfAbsent(fieldName, definitionMap.get(defnName));
                } else {
                    logger.warn("{} definition not found for field {}", defnName, fieldName);
                }
            });
        });

        logger.info("loaded schema resource(s): " + definitionMap.size());
    }

    /**
     * Returns the title for all definitions loaded
     * 
     * @return
     */
    public Set<String> getAllKnownDefinitions() {
        return definitionMap.keySet();
    }

    /**
     * Returns all definitions that are loaded
     * 
     * @return
     */
    public List<Definition> getAllDefinitions() {
        List<Definition> definitions = new ArrayList<>();
        for (Entry<String, Definition> entry : definitionMap.entrySet()) {
            definitions.add(entry.getValue());
        }
        return definitions;
    }

    /**
     * Provide a definition by given title which is already loaded
     * 
     * @param title
     * @return
     */
    public Definition getDefinition(String title) {
        return definitionMap.getOrDefault(title, null);
    }

    /**
     * Returns the map, where key is the index and value is the public fields
     *
     * @return
     * */
    public Map<String, Set<String>> getPublicFieldsInfoMap() {
        Map<String, Set<String>> result = new HashMap<>();
        for (String index: getAllKnownDefinitions()) {
            List<String> publicFields = getDefinition(index)
                    .getOsSchemaConfiguration()
                    .getPublicFields();
            if(publicFields != null) {
                result.put(index.toLowerCase(), new HashSet<>(publicFields));
            } else {
                result.put(index.toLowerCase(), Collections.emptySet());
            }
        }
        return result;
    };

    /**
     * Returns the map, where key is the index and value is the internal fields
     *
     * @return
     * */
    public Map<String, Set<String>> getExcludingFields() {
        Map<String, Set<String>> result = new HashMap<>();
        for (String index: getAllKnownDefinitions()) {
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

    public String getSubjectPath(String title) {
        return definitionMap.get(title).getOsSchemaConfiguration().getSubjectJsonPath();
    }

    public List<AttestationPolicy> getAttestationPolicy(String entityType) {
        return new ArrayList<>(definitionMap.get(entityType)
                .getOsSchemaConfiguration()
                .getAttestationPolicies());
    }

    public List<OwnershipsAttributes> getOwnershipAttributes(String entity) {
        Definition entityDefinition = definitionMap.get(entity);
        if (entityDefinition != null) {
            return entityDefinition.getOsSchemaConfiguration().getOwnershipAttributes();
        } else {
            return Collections.emptyList();
        }
    }

    public Map<String, Object> getCredentialTemplate(String entityName) {
        return getDefinition(entityName).getOsSchemaConfiguration().getCredentialTemplate();
    }
}
