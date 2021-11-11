package io.opensaber.registry.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.registry.service.ICertificateService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class CertificateServiceImpl implements ICertificateService {
    private final String templateUrl;
    private final String certificateUrl;

    public CertificateServiceImpl(@Value("${certificate.templateUrl}") String templateUrl, @Value("${certificate.pdfUrl}") String certificateUrl) {
        this.templateUrl = templateUrl;
        this.certificateUrl = certificateUrl;
    }

    @Override
    public Object getPdf(JsonNode certificateData) {
        Map<String, Object> requestBody = new HashMap<String, Object>(){{
            put("templateUrl", templateUrl);
            put("certificate", certificateData.toString());
        }};
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.postForObject(certificateUrl, requestBody, byte[].class);
    }
}
