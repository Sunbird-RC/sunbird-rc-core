package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.pojos.HealthIndicator;
import dev.sunbirdrc.registry.dao.NotFoundException;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.service.impl.RetryRestTemplate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;

import static dev.sunbirdrc.registry.middleware.util.Constants.CONNECTION_FAILURE;

public class CredentialService implements HealthIndicator {
    private static final Logger logger = LoggerFactory.getLogger(CredentialService.class);
    @Value("${signature.v2.healthCheckURL}")
    private String healthCheckUrl;
    @Value("${signature.v2.issueCredentialURL}")
    private String issueCredentialURL;
    @Value("${signature.v2.getCredentialByIdURL}")
    private String getCredentialByIdURL;
    @Value("${signature.v2.deleteCredentialByIdURL}")
    private String deleteCredentialByIdURL;
    @Value("${signature.v2.verifyCredentialURL}")
    private String verifyCredentialURL;
    @Value("${signature.v2.getRevocationListURL}")
    private String getRevocationListURL;

    private static final String credentialMethod = "rcw";
    private static final String credentialIssuerMethod = "abc";

    @Autowired
    protected RetryRestTemplate retryRestTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CredentialSchemaService credentialSchemaService;
    @Autowired
    private dev.sunbirdrc.registry.service.DIDService DIDService;

    public JsonNode issueCredential(String title, Object credentialTemplate, JsonNode input) throws Exception {
        // Render the credential using credential template
        Handlebars hb = new Handlebars();
        String templateJsonString = null;
        if(credentialTemplate instanceof LinkedHashMap || credentialTemplate instanceof JsonNode) {
            templateJsonString = JSONUtil.convertObjectJsonString(credentialTemplate);
        } else {
            templateJsonString = (String) credentialTemplate;
        }
        Template template = hb.compileInline(templateJsonString);
        String credString = template.apply(JSONUtil.convertJsonNodeToMap(input));
        ObjectNode credential = (ObjectNode) objectMapper.readTree(credString);

        // Fetch the credentials schema to get credential schema id and version
        JsonNode credSchema = credentialSchemaService.getLatestSchemaByTags(Collections.singletonList(title));
        if (credSchema == null) throw new NotFoundException("CredentialSchema", title);
        JsonNode credSchemaDid = credSchema.get("schema").get("id");
        JsonNode credSchemaVersion = credSchema.get("schema").get("version");


        // ensure issuer did
        String issuerDid = DIDService.ensureDidForName(credential.get("issuer").asText(), credentialIssuerMethod);
        credential.set("issuer", JsonNodeFactory.instance.textNode(issuerDid));

        // Wire the create credential request payload
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.set("credential", credential);
        node.set("credentialSchemaId", credSchemaDid);
        node.set("credentialSchemaVersion", credSchemaVersion);
        node.set("method", JsonNodeFactory.instance.textNode(credentialMethod));
        ArrayNode tags = JsonNodeFactory.instance.arrayNode();
        tags.add(title);
        if(input.get("osid") != null) tags.add(input.get("osid"));
        node.set("tags", tags);

        // send the request and issue credential
        HttpEntity<String> request = createPayloadFromJsonNode(node);
        ResponseEntity<String> response = retryRestTemplate.postForEntity(issueCredentialURL, request);
        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode result = JSONUtil.convertStringJsonNode(response.getBody());
            return result.get("credential");
        }
        throw new RuntimeException("Unable to issue credential");
    }

    public JsonNode getCredentialById(String credentialId) throws IOException, NotFoundException {
        ResponseEntity<String> response = retryRestTemplate.getForEntity(getCredentialByIdURL, credentialId);
        if (response.getStatusCode().is2xxSuccessful()) {
            return JSONUtil.convertStringJsonNode(response.getBody());
        }
        throw new NotFoundException("Credential", credentialId);
    }

    public byte[] getCredentialById(String credentialId, String format, String templateId, String template) throws IOException, NotFoundException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("templateId", templateId);
        if(template != null) headers.set("template", template.trim());
        headers.setAccept(Collections.singletonList(MediaType.valueOf(format)));
        ResponseEntity<byte[]> response = retryRestTemplate.getForObject(getCredentialByIdURL, headers, byte[].class, credentialId);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }
        throw new RuntimeException("Unable to render the credential");
    }

    public void revokeCredential(String credentialId) throws IOException {
        retryRestTemplate.deleteForEntity(deleteCredentialByIdURL, credentialId);
    }

    public ArrayNode revocationList(String issuerDid, Integer page, Integer limit) throws IOException {
        if(page != null && page < 1) page = 1;
        if(limit != null && limit < 1) limit = 1000;
        ResponseEntity<String> response = retryRestTemplate.getForEntity(getRevocationListURL, issuerDid, page, limit);
        if (response.getStatusCode().is2xxSuccessful()) {
            return (ArrayNode) JSONUtil.convertStringJsonNode(response.getBody());
        }
        return JsonNodeFactory.instance.arrayNode();
    }

    public JsonNode verifyCredential(String credentialId) throws IOException {
        ResponseEntity<String> response = retryRestTemplate.getForEntity(verifyCredentialURL, credentialId);
        if (response.getStatusCode().is2xxSuccessful()) {
            return JSONUtil.convertStringJsonNode(response.getBody());
        }
        return JsonNodeFactory.instance.objectNode();
    }

    @Override
    public String getServiceName() {
        return "CREDENTIAL_SERVICE";
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

    public HttpEntity<String> createPayloadFromJsonNode(JsonNode node) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(node.toString(), headers);
    }
}
