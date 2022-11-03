package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import dev.sunbirdrc.pojos.HealthIndicator;
import org.springframework.http.MediaType;

public interface ICertificateService extends HealthIndicator {
    Object getCertificate(JsonNode certificateData, String entityName, String entityId, String mediaType, String templateUrl);
}
