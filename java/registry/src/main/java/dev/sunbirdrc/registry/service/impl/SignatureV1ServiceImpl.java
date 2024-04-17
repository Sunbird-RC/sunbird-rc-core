package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.registry.exception.SignatureException;
import dev.sunbirdrc.registry.service.SignatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.Map;

import static dev.sunbirdrc.registry.middleware.util.Constants.SUNBIRD_SIGNATURE_SERVICE_NAME;

@Component
@ConditionalOnExpression("${signature.enabled:false} && ('${signature.provider}' == 'dev.sunbirdrc.registry.service.impl.SignatureV1ServiceImpl')")
public class SignatureV1ServiceImpl implements SignatureService {
    private static Logger logger = LoggerFactory.getLogger(SignatureV1ServiceImpl.class);
    @Value("${signature.v1.healthCheckURL}")
    private String healthCheckURL;
    @Value("${signature.v1.signURL}")
    private String signURL;
    @Value("${signature.v1.verifyURL}")
    private String verifyURL;
    @Value("${signature.v1.keysURL}")
    private String keysURL;
    @Autowired
    private RetryRestTemplate retryRestTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String getServiceName() {
        return SUNBIRD_SIGNATURE_SERVICE_NAME;
    }

    @Override
    public ComponentHealthInfo getHealthInfo() throws RestClientException{
        ResponseEntity<String> response = retryRestTemplate.getForEntity(healthCheckURL);
        if (!StringUtils.isEmpty(response.getBody()) && Arrays.asList("UP", "OK").contains(response.getBody().toUpperCase())) {
            logger.debug("Signature service running !");
            return new ComponentHealthInfo(getServiceName(), true);
        } else {
            return new ComponentHealthInfo(getServiceName(), false);
        }
    }

    @Override
    public Object sign(Map<String, Object> propertyValue) {
        ResponseEntity<String> response = retryRestTemplate.postForEntity(signURL, propertyValue);
        Object result = null;
        try {
            result = objectMapper.readTree(response.getBody());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public boolean verify(Object propertyValue) throws SignatureException.UnreachableException, SignatureException.VerificationException {
        ResponseEntity<String> response = null;
        boolean result = false;
        try {
            response = retryRestTemplate.postForEntity(verifyURL, propertyValue);
            JsonNode resultNode = objectMapper.readTree(response.getBody());
            result = resultNode.get("verified").asBoolean();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public String getKey(String keyId) throws SignatureException.UnreachableException, SignatureException.KeyNotFoundException {
        ResponseEntity<String> response = retryRestTemplate.getForEntity(keysURL + "/" + keyId);
        return response.getBody();
    }

    @Override
    public void revoke(String entityName, String entityId, String signed) {
        // Nothing to do :)
    }
}
