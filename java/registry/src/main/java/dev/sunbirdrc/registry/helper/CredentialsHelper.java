package dev.sunbirdrc.registry.helper;

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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;

import static dev.sunbirdrc.registry.middleware.util.Constants.CONNECTION_FAILURE;

@Component
public class CredentialsHelper implements HealthIndicator {
    private static final Logger logger = LoggerFactory.getLogger(CredentialsHelper.class);
    @Value("${credential.healthCheckURL}")
    private String healthCheckUrl;
    @Value("${credential.issueCredentialURL}")
    private String issueCredentialURL;
    @Value("${credential.getCredentialByIdURL}")
    private String getCredentialByIdURL;
    @Value("${credential.deleteCredentialByIdURL}")
    private String deleteCredentialByIdURL;
    @Value("${credential.verifyCredentialURL}")
    private String verifyCredentialURL;
    @Value("${credential.getRevocationListURL}")
    private String getRevocationListURL;

    private static final String credentialMethod = "rcw";
    private static final String credentialIssuerMethod = "abc";

    @Autowired
    private RetryRestTemplate retryRestTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CredentialSchemaHelper credentialSchemaHelper;
    @Autowired
    private DidHelper didHelper;

    public JsonNode issueCredential(String title, Object credentialTemplate, JsonNode input) throws Exception {
        // Render the credential using credential template
        Handlebars hb = new Handlebars();
        Template template = hb.compileInline((String) credentialTemplate);
        String credString = template.apply(JSONUtil.convertJsonNodeToMap(input));
        ObjectNode credential = (ObjectNode) objectMapper.readTree(credString);

        // Fetch the credentials schema to get credential schema id and version
        JsonNode credSchema = credentialSchemaHelper.getLatestSchemaByTags(Collections.singletonList(title));
        if (credSchema == null) throw new NotFoundException("CredentialSchema", title);
        JsonNode credSchemaDid = credSchema.get("schema").get("id");
        JsonNode credSchemaVersion = credSchema.get("schema").get("version");


        // ensure issuer did
        String issuerDid = didHelper.ensureDidForName(credential.get("issuer").asText(), credentialIssuerMethod);
        credential.set("issuer", JsonNodeFactory.instance.textNode(issuerDid));

        // Wire the create credential request payload
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.set("credential", credential);
        node.set("credentialSchemaId", credSchemaDid);
        node.set("credentialSchemaVersion", credSchemaVersion);
        node.set("method", JsonNodeFactory.instance.textNode(credentialMethod));
        ArrayNode tags = JsonNodeFactory.instance.arrayNode();
        tags.add(title);
        tags.add(input.get("osid"));
        node.set("tags", tags);

        // send the request and issue credential
        HttpEntity<String> request = createPayloadFromJsonNode(node);
        ResponseEntity<String> response = retryRestTemplate.postForEntity(issueCredentialURL, request);
        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode result = JSONUtil.convertStringJsonNode(response.getBody());
            return result.get("credential");
        }
        throw new RuntimeException("Unable to create credential schema");
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
        headers.set("template", template);
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
