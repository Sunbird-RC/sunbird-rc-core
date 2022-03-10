package dev.sunbirdrc.registry.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.sunbirdrc.pojos.OwnershipsAttributes;
import dev.sunbirdrc.pojos.attestation.auto.AutoAttestationPolicy;
import dev.sunbirdrc.registry.entities.AttestationPolicy;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Holds _osconfig properties for a schema  
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class OSSchemaConfiguration {
    /**
     * holds field name(s) to be encrypted
     */
    private List<String> privateFields =  new ArrayList<>();
    /**
     * Holds field name(s) to be used for signature
     */
    private List<String> signedFields =  new ArrayList<>();
    /**
     * Holds field name(s) to be used for index
     */
    private List<String> indexFields =  new ArrayList<>();
    /**
     * Holds field name(s) to be used for unique index
     */
    private List<String> uniqueIndexFields =  new ArrayList<>();
    /**
     * Holds fields name(s) to be used for auditing
     */
    private List<String> systemFields =  new ArrayList<>();
    /**
     * Holds fields name(s) for public usage
     * */
    private List<String> publicFields = new ArrayList<>();
    /**
     * Holds fields name(s) for non-public usage
     * */
    private List<String> internalFields = new ArrayList<>();
    /**
     * Contains which are all the roles (from token) can add this resource
     */
    private List<String> roles = new ArrayList<>();

    private List<String> inviteRoles = new ArrayList<>();
    private List<String> deleteRoles = new ArrayList<>();
    /**
     * Holds field path of the subject of entity
     * */
    private String subjectJsonPath = "";

    /**
     * Holds fields paths for ownership details of the entity
     * */
    private List<OwnershipsAttributes> ownershipAttributes = new ArrayList<>();

    /**
     *
     * Holds attestableFields info,
     * Where key is the property name eg. education, certification, ...
     * and values are list of fields requires attestation
     */
    private Map<String, List<String>> attestationFields = new HashMap<>();

    /**
     * Holds the info of attestation policy
     */
    private List<AttestationPolicy> attestationPolicies = new ArrayList<>();

    /**
     * Holds the info of auto attestation policy
     */
    private List<AutoAttestationPolicy> autoAttestationPolicies = new ArrayList<>();

    /**
     * Holds the template for w3c credentials
     * */

    private Object credentialTemplate = new HashMap<>();

    /**
     * Holds the certificate template
     * */
    private Map<String, String> certificateTemplates = new HashMap<>();

    /**
     * Tells entity should be part of user service or not
     * */
    private Boolean enableLogin = true;

    private Boolean enableSearch = true;

    public String getConditions(String property) {
        return getAttestationPolicy(property).getConditions();
    }

    private AttestationPolicy getAttestationPolicy(String property) {
        return attestationPolicies.stream()
                .filter(policy -> policy.hasProperty(property))
                .findFirst().orElse(new AttestationPolicy());
    }

    public Set<String> getAllTheAttestorEntities(){
        return attestationPolicies.stream()
                .map(AttestationPolicy::getAttestorEntity)
                .collect(Collectors.toSet());
    }
    public String getAttestorEntity(String property) {
        return getAttestationPolicy(property).getAttestorEntity();
    }

    public AutoAttestationPolicy getAutoAttestationPolicy(List<String> fieldNames) {
        return autoAttestationPolicies.stream()
                .filter(policy -> fieldNames.contains(policy.getParentProperty()))
                .findFirst().orElse(new AutoAttestationPolicy());
    }

    public Optional<AttestationPolicy> getAttestationPolicyFor(String policyName) {
        return attestationPolicies.stream()
                .filter(attestationPolicy -> attestationPolicy.getName().equals(policyName))
                .findFirst();
    }
    public Boolean getEnableLogin() {
        return enableLogin;
    }

    public void setEnableLogin(Boolean enableLogin) {
        this.enableLogin = enableLogin;
    }

}
