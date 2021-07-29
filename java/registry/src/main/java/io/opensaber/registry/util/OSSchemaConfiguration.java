package io.opensaber.registry.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.opensaber.pojos.attestation.AttestationPolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds _osconfig properties for a schema  
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
    /** 
     * Holds field path of the subject of entity
     * */
    private String subjectJsonPath = "";

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

    public List<String> getPrivateFields() {
        return privateFields; 
    }

    public void setPrivateFields(List<String> privateFields) {
        this.privateFields = privateFields;
    }

    public List<String> getSignedFields() {
        return signedFields;
    }

    public void setSignedFields(List<String> signedFields) {
        this.signedFields = signedFields;
    }

    public List<String> getIndexFields() {
        return indexFields;
    }

    public void setIndexFields(List<String> indexFields) {
        this.indexFields = indexFields;
    }

    public List<String> getUniqueIndexFields() {
        return uniqueIndexFields;
    }

    public void setUniqueIndexFields(List<String> uniqueIndexFields) {
        this.uniqueIndexFields = uniqueIndexFields;
    }

    public List<String> getSystemFields() {
        return systemFields;
    }

    public void setSystemFields(List<String> systemFields) {
        this.systemFields = systemFields;
    }


    public List<String> getPublicFields() {
        return publicFields;
    }

    public List<String> getInternalFields() {
        return internalFields;
    }

    public void setPublicFields(List<String> publicFields) {
        this.publicFields = publicFields;
    }

    public void setInternalFields(List<String> internalFields) {
        this.internalFields = internalFields;
    }

    public String getSubjectJsonPath() {
        return subjectJsonPath;
    }

    public void setSubjectJsonPath(String subjectJsonPath) {
        this.subjectJsonPath = subjectJsonPath;
    }

    public Map<String, List<String>> getAttestationFields() {
        return attestationFields;
    }

    public void setAttestationFields(Map<String, List<String>> attestationFields) {
        this.attestationFields = attestationFields;
    }

    public List<AttestationPolicy> getAttestationPolicies() {
        return attestationPolicies;
    }

    public void setAttestationPolicies(List<AttestationPolicy> attestationPolicies) {
        this.attestationPolicies = attestationPolicies;
    }

    public String getConditions(String property) {
        return getAttestationPolicy(property).getConditions();
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    private AttestationPolicy getAttestationPolicy(String property) {
        return attestationPolicies.stream()
                .filter(policy -> policy.hasProperty(property))
                .findFirst().orElse(new AttestationPolicy());
    }

    public String getAttestorEntity(String property) {
        return getAttestationPolicy(property).getAttestorEntity();
    }
}
