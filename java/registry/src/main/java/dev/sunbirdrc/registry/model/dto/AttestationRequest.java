package dev.sunbirdrc.registry.model.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.HashMap;
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
//	private Map<String, List<String>> propertiesOSID;
	private JsonNode propertyData;
	private String emailId;
	private String osCreatedAt;
	private Map<String, Object> additionalProperties = new HashMap<>();

	@JsonAnySetter
	public void setAdditionalProperty(String name, Object value) {
		additionalProperties.put(name, value);
	}

	@JsonAnyGetter
	public Map<String, Object> getAdditionalProperties() {
		return additionalProperties;
	}

	public Object getProperty(String name) {
		return additionalProperties.get(name);
	}

	public static String PropertiesUUIDKey(String uuidPropertyName) {
		return "properties" + uuidPropertyName.toUpperCase();
	}

	public Map<String, List<String>> getPropertiesUUID(String uuidPropertyName) {
		return (Map<String, List<String>>) getProperty(PropertiesUUIDKey(uuidPropertyName));
	}
}
