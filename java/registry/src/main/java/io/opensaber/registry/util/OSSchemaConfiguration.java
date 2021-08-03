package io.opensaber.registry.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.opensaber.pojos.OwnershipsAttributes;
import io.opensaber.pojos.attestation.AttestationPolicy;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


    public String getConditions(String property) {
        return getAttestationPolicy(property).getConditions();
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
