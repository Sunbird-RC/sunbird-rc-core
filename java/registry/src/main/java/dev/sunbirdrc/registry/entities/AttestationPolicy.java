package dev.sunbirdrc.registry.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttestationPolicy {
    private static final Logger logger = LoggerFactory.getLogger(AttestationPolicy.class);

    private String osid;

	private final static String PLUGIN_SPLITTER = ":";

	/**
	 * name property will be used to pick the specific attestation policy
	 */
	private String name;
	/*
	 * Holds the name of the attestation property. eg. education, certificate, course
	 *
	 * */
	private Object attestationProperties;
	/**
	 * Holds the info of manual or automated attestation
	 */
	private AttestationType type;
	/*
	 * Holds the expression to identify the attestor
	 * */
	private String conditions;
	/*
	 * It will be used to define the actor name
	 * */
	private String attestorPlugin;
	/*
	 * It will be used for signin redirection eg. consent based screens
	 * */
	private String attestorSignin;
	/*
	 * Credential template for an attestation
	 * */
	private Object credentialTemplate;

	private String entity;

	private Date updatedAt;

	private String createdBy;

	private AttestationStatus status;

	private Map<String, Object> additionalInput;

	private List<AttestationStep> attestationSteps;

	private String onComplete;

	public String getAttestorEntity() {
		String[] split = this.attestorPlugin.split("entity=");
		return split.length == 2 ? split[1] : "";
	}

	public String getNodePath() {
		return name + "/[]";
	}

	public boolean isInternal() {
		return this.attestorPlugin.split(PLUGIN_SPLITTER)[1].equals(AttestorPluginType.internal.name());
	}

	public Map<String, String> getAttestationProperties() {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			TypeReference<Map<String, String>> typeRef
					= new TypeReference<Map<String, String>>() {
			};
			return objectMapper.readValue(objectMapper.writeValueAsString(this.attestationProperties), typeRef);
		} catch (Exception e) {
			return Collections.emptyMap();
		}
	}

	public FlowType getCompletionType() {
		if (!StringUtils.isEmpty(this.onComplete)) {
            try {
                return FlowType.valueOf(this.onComplete.split(":")[0].toUpperCase());
            } catch (Exception e) {
                logger.error("Invalid value for onComplete field: {}", this.onComplete, e);
            }
		}
		return FlowType.NONE;
	}

	public String getCompletionValue() {
		if (!StringUtils.isEmpty(this.onComplete)) {
			return this.onComplete.split(":")[1];
		}
		return "";
	}

	public String getCompletionFunctionName() {
		String completionValue = this.getCompletionValue();
		return StringUtils.substring(completionValue,
				completionValue.lastIndexOf("/") + 1,
                completionValue.contains("(") ? completionValue.indexOf("(") : completionValue.length());
	}
}
