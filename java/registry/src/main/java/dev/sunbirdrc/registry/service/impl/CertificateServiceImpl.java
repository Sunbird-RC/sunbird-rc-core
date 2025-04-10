package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.service.ICertificateService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static dev.sunbirdrc.registry.middleware.util.Constants.CONNECTION_FAILURE;
import static dev.sunbirdrc.registry.middleware.util.Constants.SUNBIRD_CERTIFICATE_SERVICE_NAME;

@Component
@ConditionalOnExpression("${certificate.enabled:false} && ('${signature.provider}' == 'dev.sunbirdrc.registry.service.impl.SignatureV1ServiceImpl')")
public class CertificateServiceImpl implements ICertificateService {
    private static Logger logger = LoggerFactory.getLogger(CertificateServiceImpl.class);
    private final String templateBaseUrl;
    private final String certificateUrl;
    private final String certificateHealthCheckURL;
    private final RestTemplate restTemplate;
    private boolean signatureEnabled;

    public CertificateServiceImpl(@Value("${certificate.templateBaseUrl}") String templateBaseUrl,
                                  @Value("${certificate.apiUrl}") String certificateUrl,
                                  @Value("${signature.enabled}") boolean signatureEnabled,
                                  @Value("${certificate.healthCheckURL}") String certificateHealthCheckURL,
                                  RestTemplate restTemplate) {
        this.templateBaseUrl = templateBaseUrl;
        this.certificateUrl = certificateUrl;
        this.restTemplate = restTemplate;
        this.certificateHealthCheckURL = certificateHealthCheckURL;
        this.signatureEnabled = signatureEnabled;
    }

    @Override
    public Object getCertificate(JsonNode certificateData, String entityName, String entityId, String mediaType, String templateUrl, String templateId, JsonNode entity) throws RestClientException, CertificateException {
        if (Objects.equals(mediaType, MediaType.APPLICATION_JSON_VALUE)) {
            try {
                return new ObjectMapper().readTree(certificateData.asText());
            } catch (JsonProcessingException e) {
                throw new CertificateException(e.getMessage());
            }
        }
        String finalTemplateUrl = inferTemplateUrl(entityName, mediaType, templateUrl);

        Map<String, Object> requestBody = new HashMap<String, Object>() {{
            put("templateUrl", finalTemplateUrl);
            put("certificate", certificateData.toString());
            put("entityId", entityId);
            put("entityName", entityName);
            put("entity", entity);
        }};
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", mediaType);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);
        return restTemplate.postForObject(certificateUrl, httpEntity, byte[].class);
    }

    @NotNull
    private String inferTemplateUrl(String entityName, String mediaType, String templateUrl) {
        if (templateUrl == null) {
            templateUrl = templateBaseUrl + entityName + getFileExtension(mediaType);
        }
        return templateUrl;
    }

    @NotNull
    private String getFileExtension(String mediaType) {
        if (Constants.SVG_MEDIA_TYPE.equals(mediaType)) {
            return ".svg";
        }
        return ".html";
    }

    @Override
    public String getServiceName() {
        return SUNBIRD_CERTIFICATE_SERVICE_NAME;
    }

    @Override
    public ComponentHealthInfo getHealthInfo() {
        if (signatureEnabled) {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(URI.create(certificateHealthCheckURL), String.class);
                if (!StringUtils.isEmpty(response.getBody()) && Arrays.asList("UP", "OK").contains(response.getBody().toUpperCase())) {
                    logger.debug("Certificate service running !");
                    return new ComponentHealthInfo(getServiceName(), true);
                } else {
                    return new ComponentHealthInfo(getServiceName(), false);
                }
            } catch (RestClientException ex) {
                logger.error("RestClientException when checking the health of the certificate service: {}", ExceptionUtils.getStackTrace(ex));
                return new ComponentHealthInfo(getServiceName(), false, CONNECTION_FAILURE, ex.getMessage());
            }
        } else {
            return new ComponentHealthInfo(getServiceName(), true, "SIGNATURE_ENABLED", "false");
        }

    }
}
