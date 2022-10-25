package dev.sunbirdrc.registry.entities;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class VerificationRequest {
	JsonNode signedCredentials;
}
