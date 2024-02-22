package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.pojos.HealthIndicator;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.service.impl.RetryRestTemplate;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static dev.sunbirdrc.registry.middleware.util.Constants.CONNECTION_FAILURE;

@Component
@ConditionalOnExpression("${signature.enabled:false} && ('${signature.provider}' == 'dev.sunbirdrc.registry.service.impl.SignatureV2ServiceImpl')")
public class CredentialSchemaService implements HealthIndicator {
    private static final Logger logger = LoggerFactory.getLogger(CredentialSchemaService.class);
    @Value("${signature.v2.schema.healthCheckURL}")
    private String healthCheckUrl;
    @Value("${signature.v2.schema.createSchemaURL}")
    private String createUrl;
    @Value("${signature.v2.schema.updateSchemaURL}")
    private String updateUrl;
    @Value("${signature.v2.schema.getSchemaByIdAndVersionURL}")
    private String getByIdAndVersionUrl;
    @Value("${signature.v2.schema.getSchemaByTagsURL}")
    private String getByTagsUrl;

    @Autowired(required = false)
    dev.sunbirdrc.registry.service.DIDService DIDService;
    @Autowired
    IDefinitionsManager definitionsManager;

    private static final String authorName = "Registry";
    private static final String authorDidMethod = "abc";

    @Autowired
    private RetryRestTemplate retryRestTemplate;

    public JsonNode convertCredentialTemplateToSchema(String title, Object credTemplate) throws IOException {
        String name = "Proof of " + title + " Credential";
        String schemaId = "Proof-of-" + title + "-Credential";
        String templateJsonString = null;
        if(credTemplate instanceof LinkedHashMap || credTemplate instanceof JsonNode) {
            templateJsonString = JSONUtil.convertObjectJsonString(credTemplate);
        } else {
            templateJsonString = (String) credTemplate;
        }
        JsonNode credSchema = JSONUtil.convertStringJsonNode("{\"type\":\"https://w3c-ccg.github.io/vc-json-schemas/\",\"version\":\"1.0.0\",\"name\":\""+name+"\",\"author\":\"\",\"authored\":\"\",\"schema\":{\"$id\":\""+schemaId+"\",\"$schema\":\"https://json-schema.org/draft/2019-09/schema\",\"description\":\"\",\"type\":\"object\",\"properties\":{},\"required\":[],\"additionalProperties\":false}}");
        JsonNode node = JSONUtil.convertStringJsonNode(templateJsonString);
        JsonNode subject = node.get("credentialSubject");
        JsonNode schemaProperties = credSchema.get("schema").get("properties");
        ArrayNode required = (ArrayNode) credSchema.get("schema").get("required");
        subject.fields().forEachRemaining(field -> {
            String key = field.getKey();
            required.add(key);
            ObjectNode value = JsonNodeFactory.instance.objectNode();
            value.put("type", "string");
            ((ObjectNode) schemaProperties).set(key, value);
        });
        return credSchema;
    }

    public void ensureCredentialSchemas() {
        this.definitionsManager.getAllDefinitions()
                .forEach(definition -> {
                    try {
                        Object credTemplate = definition.getOsSchemaConfiguration().getCredentialTemplate();
                        if(credTemplate != null) {
                            this.ensureCredentialSchema(
                                    definition.getTitle(),
                                    credTemplate, null);
                            logger.info("Ensured credential schema for definition: {}", definition.getTitle());
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public void ensureCredentialSchema(String title, Object credTemplate, String status) throws Exception {
        JsonNode schema = convertCredentialTemplateToSchema(title, credTemplate);
        ObjectNode prevSchema = (ObjectNode) getLatestSchemaByTags(Collections.singletonList(title));
        String author = DIDService.ensureDidForName(authorName, authorDidMethod);
        String authored = Instant.now().toString();
        ((ObjectNode) schema).set("author", JsonNodeFactory.instance.textNode(author));
        ((ObjectNode) schema).set("authored", JsonNodeFactory.instance.textNode(authored));
        if (prevSchema == null) {
            createSchema(title, schema, status);
        } else {
            ObjectNode prevProps = (ObjectNode) prevSchema.get("schema").get("schema").get("properties");
            ObjectNode currProps = (ObjectNode) schema.get("schema").get("properties");
            AtomicBoolean updateRequired = new AtomicBoolean(false);
            if(status != null) updateRequired.set(!prevSchema.get("status").asText().equals(status));
            if(!updateRequired.get()) currProps.fieldNames().forEachRemaining(d -> updateRequired.set(updateRequired.get() || !prevProps.has(d)));
            if(!updateRequired.get()) prevProps.fieldNames().forEachRemaining(d -> updateRequired.set(updateRequired.get() || !currProps.has(d)));
            String did = prevSchema.get("schema").get("id").asText();
            String version = prevSchema.get("schema").get("version").asText();
            if(updateRequired.get()) {
                if(status == null) status = prevSchema.get("status").asText();
                updateSchema(did, version, schema, status);
            }
        }
    }

    public JsonNode createSchema(String title, JsonNode credentialSchema, String status) throws IOException {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.set("schema", credentialSchema);
        ArrayNode tags = JsonNodeFactory.instance.arrayNode();
        tags.add(title);
        node.set("tags", tags);
        if(status == null) status = "DRAFT";
        node.set("status", JsonNodeFactory.instance.textNode(status));
        HttpEntity<String> request = createPayloadFromJsonNode(node);
        ResponseEntity<String> response = retryRestTemplate.postForEntity(createUrl, request);
        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode result = JSONUtil.convertStringJsonNode(response.getBody());
            return result.get("schema");
        }
        throw new RuntimeException("Unable to create credential schema");
    }

    public JsonNode updateSchema(String did, String version, JsonNode credentialSchema, String status) throws IOException {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.set("schema", credentialSchema);
        node.set("status", JsonNodeFactory.instance.textNode(status));
        HttpEntity<String> request = createPayloadFromJsonNode(node);
        ResponseEntity<String> response = retryRestTemplate.putForEntity(updateUrl, request, did, version);
        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode result = JSONUtil.convertStringJsonNode(response.getBody());
            return result.get("schema");
        }
        throw new RuntimeException("Unable to create credential schema");
    }

    public JsonNode getLatestSchemaByTags(List<String> tags) throws IOException {
        ArrayNode schemas = getSchemaByTags(tags);
        final AtomicReference<JsonNode> latestSchema = new AtomicReference<>();
        List<String> discardedSchemaStatus = Arrays.asList("DEPRECATED", "REVOKED");
        schemas.forEach(d -> {
            if(!discardedSchemaStatus.contains(d.get("status").asText())) {
                if(latestSchema.get() == null || d.get("schema").get("version").asText()
                        .compareTo(latestSchema.get().get("schema").get("version").asText()) > 0) {
                    latestSchema.set(d);
                }
            }
        });
        return latestSchema.get();
    }

    public JsonNode getSchemaByIdAndVersion(String did, String version) throws IOException {
        ResponseEntity<String> response = retryRestTemplate.getForEntity(getByIdAndVersionUrl, did, version);
        if (response.getStatusCode().is2xxSuccessful()) {
            return JSONUtil.convertStringJsonNode(response.getBody());
        }
        throw new RuntimeException("Unable to fetch credentials schema by did and version");
    }

    public ArrayNode getSchemaByTags(List<String> tags) throws IOException {
        ResponseEntity<String> response = retryRestTemplate.getForEntity(getByTagsUrl, String.join(",", tags));
        if (response.getStatusCode().is2xxSuccessful()) {
            return (ArrayNode) JSONUtil.convertStringJsonNode(response.getBody());
        }
        return JsonNodeFactory.instance.arrayNode();
    }

    public HttpEntity<String> createPayloadFromJsonNode(JsonNode node) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(node.toString(), headers);
    }


    @Override
    public String getServiceName() {
        return "CREDENTIAL_SCHEMA_SERVICE";
    }

    @Override
    public ComponentHealthInfo getHealthInfo() {
        try {
            ResponseEntity<String> response = retryRestTemplate.getForEntity(healthCheckUrl);
            if (!StringUtils.isEmpty(response.getBody()) && JSONUtil.convertStringJsonNode(response.getBody()).get("status").asText().equalsIgnoreCase("UP")) {
                logger.debug("{} service running!", this.getServiceName());
                return new ComponentHealthInfo(getServiceName(), true);
            } else {
                return new ComponentHealthInfo(getServiceName(), false);
            }
        } catch (Exception e) {
            logger.error("Exception when checking the health of the Sunbird {} service: {}", getServiceName(), ExceptionUtils.getStackTrace(e));
            return new ComponentHealthInfo(getServiceName(), false, CONNECTION_FAILURE, e.getMessage());
        }
    }
}
