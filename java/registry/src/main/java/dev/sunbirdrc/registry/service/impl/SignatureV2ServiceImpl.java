package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.registry.dao.NotFoundException;
import dev.sunbirdrc.registry.exception.SignatureException;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.service.CredentialSchemaService;
import dev.sunbirdrc.registry.service.DIDService;
import dev.sunbirdrc.registry.service.ICertificateService;
import dev.sunbirdrc.registry.service.SignatureService;
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
import java.net.URLDecoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static dev.sunbirdrc.registry.Constants.*;
import static dev.sunbirdrc.registry.middleware.util.Constants.CONNECTION_FAILURE;

@Component
@ConditionalOnExpression("${signature.enabled:false} && ('${signature.provider-name}' == 'dev.sunbirdrc.registry.service.impl.SignatureV2ServiceImpl')")
public class SignatureV2ServiceImpl implements SignatureService, ICertificateService {
    private static final Logger logger = LoggerFactory.getLogger(SignatureV2ServiceImpl.class);
    @Value("${signature.v2.health-check-url}")
    private String healthCheckUrl;
    @Value("${signature.v2.issue-url}")
    private String issueCredentialURL;
    @Value("${signature.v2.get-url}")
    private String getCredentialByIdURL;
    @Value("${signature.v2.delete-url}")
    private String deleteCredentialByIdURL;
    @Value("${signature.v2.verify-url}")
    private String verifyCredentialURL;
    @Value("${signature.v2.revocation-list-url}")
    private String getRevocationListURL;

    @Value("${signature.v2.credential-did-method}")
    private String credentialMethod;
    @Value("${signature.v2.issuer-did-method}")
    private String credentialIssuerMethod;

    @Value("${database.uuidPropertyName}")
    public String uuidPropertyName;

    @Autowired
    protected RetryRestTemplate retryRestTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CredentialSchemaService credentialSchemaService;
    @Autowired
    private DIDService didService;

    @Override
    public Object sign(Map<String, Object> propertyValue) throws SignatureException.UnreachableException, SignatureException.CreationException {
        String title = (String) propertyValue.get("title");
        JsonNode data =(JsonNode) (propertyValue.get("data"));
        Object credentialTemplate = propertyValue.get("credentialTemplate");
        try {
            return this.issueCredential(title, credentialTemplate, data);
        } catch (Exception e) {
            logger.error("Exception occurred while issuing a credential for {}: {}", title, ExceptionUtils.getStackTrace(e));
            throw new SignatureException.CreationException(e.getMessage());
        }
    }

    @Override
    public boolean verify(Object propertyValue) throws SignatureException.UnreachableException, SignatureException.VerificationException {
        String credentialId = (String) ((Map<String, Object>) propertyValue).get("credentialId");
        ObjectNode credential = (ObjectNode) ((Map<String, Object>) propertyValue).get("signedCredentials");
        if(credentialId == null || credentialId.isEmpty()) {
            credentialId = credential.get("credentialId").asText();
        }
        JsonNode resultNode = null;
        try {
            resultNode = this.verifyCredential(credentialId);
        } catch (IOException e) {
            throw new SignatureException.VerificationException(e.getMessage());
        }
        return resultNode.get("verified").asBoolean();
    }

    @Override
    public String getKey(String keyId) throws SignatureException.UnreachableException, SignatureException.KeyNotFoundException {
        ObjectNode didDocument = (ObjectNode) didService.resolveDid(keyId);
        ArrayNode verificationMethods = (ArrayNode) didDocument.get("verificationMethod");
        AtomicReference<JsonNode> verificationMethod = new AtomicReference<>();
        verificationMethods.elements().forEachRemaining(vm -> {
            if(vm.get("id").asText().equals(keyId)) {
                verificationMethod.set(vm);
            }
        });
        return verificationMethod.get() != null ? verificationMethod.get().toString() : null;
    }

    @Override
    public void revoke(String entityName, String entityId, String signed) {
        try {
            this.revokeCredential(signed);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getCertificate(JsonNode credentialId, String entityName, String entityId, String mediaType, String template, String templateId, JsonNode entity) throws Exception {
        if (Objects.equals(mediaType, MediaType.APPLICATION_JSON.toString())) {
            return getCredentialById(credentialId.asText());
        }
        if(template != null && (template.startsWith(HTTP_URI_PREFIX) || template.startsWith(HTTPS_URI_PREFIX))) {
            ResponseEntity<String> response = this.retryRestTemplate.getForEntity(URLDecoder.decode(template, "UTF-8"));
            template = response.getBody();
        }
        return getCredentialById(credentialId.asText(), mediaType, templateId, template);
    }

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
        String issuerDid = didService.ensureDidForName(credential.get("issuer").asText(), credentialIssuerMethod);
        credential.set("issuer", JsonNodeFactory.instance.textNode(issuerDid));

        // Wire the create credential request payload
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.set("credential", credential);
        node.set("credentialSchemaId", credSchemaDid);
        node.set("credentialSchemaVersion", credSchemaVersion);
        node.set("method", JsonNodeFactory.instance.textNode(credentialMethod));
        ArrayNode tags = JsonNodeFactory.instance.arrayNode();
        tags.add(title);
        if(input.get(uuidPropertyName) != null) tags.add(input.get(uuidPropertyName));
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
        if(template != null) headers.set(Template, template.trim());
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
            JsonNode responseBody = JSONUtil.convertStringJsonNode(response.getBody());
            if (!StringUtils.isEmpty(response.getBody()) && Stream.of("OK", "UP").anyMatch(d -> d.equalsIgnoreCase(responseBody.get("status").asText()))) {
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
