package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import dev.sunbirdrc.pojos.HealthIndicator;

public interface ICertificateService extends HealthIndicator {
    Object getCertificate(JsonNode certificateData, String entityName, String entityId, String mediaType, String templateUrl, JsonNode entity, String fileName, boolean wc);
}
