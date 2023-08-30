package dev.sunbirdrc.registry.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttestationRequest {
	private String entityName;
	private String entityId;
	private String name;
	private String userId;
	private JsonNode additionalInput;
	private Map<String, List<String>> propertiesOSID;
	private JsonNode propertyData;
	private String emailId;
	private String credType;
}
