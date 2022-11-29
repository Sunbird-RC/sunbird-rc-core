package dev.sunbirdrc.registry.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.pojos.OwnershipsAttributes;
import dev.sunbirdrc.registry.middleware.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

import static dev.sunbirdrc.registry.Constants.TITLE;

public class DistributedDefinitionsManager implements IDefinitionsManager {

    private static final String SCHEMA = "SCHEMA_";
    private static final String SCHEMA_WILDCARD = SCHEMA + "*";
    @Autowired
    private JedisPool jedisPool;
    @Autowired
    private ObjectMapper objectMapper;
    private OSResourceLoader osResourceLoader;
    private static final Logger logger = LoggerFactory.getLogger(DistributedDefinitionsManager.class);
    @Autowired
    private ResourceLoader resourceLoader;

    @PostConstruct
    @Override
    public void loadDefinition() throws Exception {
        loadResourcesFromPath(Constants.RESOURCE_LOCATION);
        loadResourcesFromPath(Constants.INTERNAL_RESOURCE_LOCATION);
        logger.info("loaded schema resource(s): ");
    }

    private void loadResourcesFromPath(String resourceLocation) throws Exception {
        osResourceLoader = new OSResourceLoader(resourceLoader);
        osResourceLoader.loadResource(resourceLocation);

        for (Map.Entry<String, String> entry : osResourceLoader.getNameContent().entrySet()) {
            String filename = entry.getKey();
            String filenameWithoutExtn = filename.substring(0, filename.indexOf('.'));
            String schemaAsText = entry.getValue();
            JsonNode schemaJson = objectMapper.readTree(schemaAsText);
            Definition definition = new Definition(schemaJson);
            try(Jedis jedis = jedisPool.getResource()) {
                jedis.set(SCHEMA + definition.getTitle(), schemaAsText);
                jedis.set(SCHEMA + filenameWithoutExtn, schemaAsText);
            }

            logger.info("loading resource:" + entry.getKey() + " with private field size:"
                    + definition.getOsSchemaConfiguration().getPrivateFields().size() + " & signed fields size:"
                    + definition.getOsSchemaConfiguration().getSignedFields().size());
        }
    }

    @Override
    public Set<String> getAllKnownDefinitions() {
        try(Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys(SCHEMA_WILDCARD);
            keys = keys.stream().map(key -> key.substring(SCHEMA.length())).collect(Collectors.toSet());
            return keys;
        }
    }

    @Override
    public List<Definition> getAllDefinitions() {
        try(Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys(SCHEMA_WILDCARD);
            String[] keysArr = keys.toArray(new String[0]);
            List<String> definitionsStr = jedis.mget(keysArr);
            List<Definition> definitions = new ArrayList<>();
            for(String definitionStr : definitionsStr) {
                JsonNode jsonNode = objectMapper.readTree(definitionStr);
                Definition definition = new Definition(jsonNode);
                definitions.add(definition);
            }
            return definitions;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Definition getDefinition(String title) {
        try(Jedis jedis = jedisPool.getResource()) {
            String schemaAsText = jedis.get(SCHEMA + title);
            if(schemaAsText == null) {
                return null;
            }
            JsonNode schemaNode = objectMapper.readTree(schemaAsText);
            return new Definition(schemaNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Definition> getDefinitionMap() {
        Map<String, Definition> definitionMap = new HashMap<>();
        try(Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys(SCHEMA_WILDCARD);
            String[] keysArr = keys.toArray(new String[0]);
            List<String> definitionsStr = jedis.mget(keysArr);
            for (int i = 0; i < definitionsStr.size(); i++) {
                String definitionStr = definitionsStr.get(i);
                JsonNode jsonNode = objectMapper.readTree(definitionStr);
                Definition definition = new Definition(jsonNode);
                definitionMap.put(keysArr[i], definition);
            }
            return definitionMap;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<OwnershipsAttributes> getOwnershipAttributes(String entity) {
        try(Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(SCHEMA + entity);
            if(value != null) {
                JsonNode schemaJson = objectMapper.readTree(value);
                Definition definition = new Definition(schemaJson);
                return definition.getOsSchemaConfiguration().getOwnershipAttributes();
            }
            return Collections.emptyList();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (NullPointerException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isValidEntityName(String entityName) {
        try(Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(SCHEMA + entityName);
        }
    }

    @Override
    public void appendNewDefinition(JsonNode jsonNode) {
        String schemaAsText = jsonNode.asText("{}");
        JsonNode schemaJsonNode = null;
        try {
            schemaJsonNode = objectMapper.readTree(schemaAsText);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Definition definition = new Definition(schemaJsonNode);
        try(Jedis jedis = jedisPool.getResource()) {
            jedis.set(SCHEMA+definition.getTitle(), jsonNode.asText("{}"));
        }
    }

    @Override
    public void removeDefinition(JsonNode jsonNode) {
        try(Jedis jedis = jedisPool.getResource()) {
            String schemaAsText = jsonNode.asText("{}");
            JsonNode schemaJsonNode = objectMapper.readTree(schemaAsText);
            String schemaTitle = SCHEMA + schemaJsonNode.get(TITLE).asText();
            jedis.del(schemaTitle);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

