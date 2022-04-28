package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.service.ICertificateService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class CertificateServiceImpl implements ICertificateService {
    private final String templateBaseUrl;
    private final String certificateUrl;
    private final RestTemplate restTemplate;
    private static Logger logger = LoggerFactory.getLogger(CertificateServiceImpl.class);

    public CertificateServiceImpl(@Value("${certificate.templateBaseUrl}") String templateBaseUrl, @Value("${certificate.apiUrl}") String certificateUrl, RestTemplate restTemplate) {
        this.templateBaseUrl = templateBaseUrl;
        this.certificateUrl = certificateUrl;
        this.restTemplate = restTemplate;
    }

    @Override
    public Object getCertificate(JsonNode certificateData, String entityName, String entityId, String mediaType, String templateUrl) {
        try {
            String finalTemplateUrl = inferTemplateUrl(entityName, mediaType, templateUrl);

            Map<String, Object> requestBody = new HashMap<String, Object>(){{
                put("templateUrl", finalTemplateUrl);
                put("certificate", certificateData.toString());
                put("entityId", entityId);
            }};
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", mediaType);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            return restTemplate.postForObject(certificateUrl, entity, byte[].class);
        } catch (Exception e) {
            logger.error("Get certificate failed", e);
        }
        return null;
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
}
