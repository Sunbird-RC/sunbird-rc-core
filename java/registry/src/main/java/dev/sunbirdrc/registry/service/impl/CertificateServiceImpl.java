package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.service.ICertificateService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class CertificateServiceImpl implements ICertificateService {
    private final String templateBaseUrl;
    private final String certificateUrl;

    public CertificateServiceImpl(@Value("${certificate.templateBaseUrl}") String templateBaseUrl, @Value("${certificate.apiUrl}") String certificateUrl) {
        this.templateBaseUrl = templateBaseUrl;
        this.certificateUrl = certificateUrl;
    }

    @Override
    public Object getCertificate(JsonNode certificateData, String entityName, String mediaType) {
        Map<String, Object> requestBody = new HashMap<String, Object>(){{
            String templateUrl = templateBaseUrl + entityName + getFileExtension(mediaType);
            put("templateUrl", templateUrl);
            put("certificate", certificateData.toString());
        }};
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", mediaType);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.postForObject(certificateUrl, entity, byte[].class);
    }

    @NotNull
    private String getFileExtension(String mediaType) {
        if (Constants.SVG_MEDIA_TYPE.equals(mediaType)) {
            return ".svg";
        }
        return ".html";
    }
}
