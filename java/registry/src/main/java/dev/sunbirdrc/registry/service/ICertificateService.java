package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;

public interface ICertificateService {
    Object getCertificate(JsonNode certificateData, String entityName, String entityId, String mediaType, String templateUrl);
}
