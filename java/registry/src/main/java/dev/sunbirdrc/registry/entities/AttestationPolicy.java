package dev.sunbirdrc.registry.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "AttestationPolicy", indexes = @Index(columnList = "entity"))
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttestationPolicy {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private String id;

    private final static String PLUGIN_SPLITTER = ":";

    /**
     * name property will be used to pick the specific attestation policy
     */
    @Column(unique = true)
    private String name;
    /*
     * Holds the name of the attestation property. eg. education, certificate, course
     *
     * */
    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<String> properties;
    /*
     * Holds the name of the attestation property. eg. education, certificate, course
     *
     * */
    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    private Map<String, String> attestationProperties;
    /**
     * Holds the value of the jsonpath
     */
    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<String> paths;
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
    private String credentialTemplateStr;

    private String additionalInputStr;

    private String entity;

    @CreatedDate
    private Date createdAt = new Date();

    @LastModifiedDate
    private Date updatedAt;

    private String createdBy;

    private AttestationStatus status;


    @Transient
    private Map<String, Object> credentialTemplate;

    @Transient
    private Map<String, Object> additionalInput;

    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<AttestationStep> attestationSteps;

    @PrePersist
    private void setInternalFields() {
        this.updatedAt = new Date();
        this.createdAt = new Date();
    }

    @PreUpdate
    private void setObjectFields() {
        this.updatedAt = new Date();
    }

    public String getAttestorEntity() {
        String a = "{\"signedCredentials\":{\"type\":\"object\"},\"documentOSID\":{\"type\":\"string\"}}";
        String[] split = this.attestorPlugin.split("entity=");
        return split.length == 2 ? split[1] : "";
    }

    public boolean hasProperty(String property) {
        return getProperties().equals(property);
    }

    public String getNodePath() {
        return name + "/[]";
    }

    public boolean isInternal() {
        return this.attestorPlugin.split(PLUGIN_SPLITTER)[1].equals(AttestorPluginType.internal.name());
    }

    public Map<String, Object> getAdditionalInput() {
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<Map<String, Object>> typeRef
                = new TypeReference<Map<String, Object>>() {
        };
        try {
            return objectMapper.readValue(this.additionalInputStr, typeRef);
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }

    }

    public Map<String, Object> getCredentialTemplate() {
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<Map<String, Object>> typeRef
                = new TypeReference<Map<String, Object>>() {
        };
        try {
            return objectMapper.readValue(this.credentialTemplateStr, typeRef);
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }

    }

    @JsonIgnore
    public Map<String, Object> getAdditionalInputs() {
        return additionalInput;
    }

    @JsonIgnore
    public Map<String, Object> getCredentialTemplates() {
        return credentialTemplate;
    }
}
