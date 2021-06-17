package io.opensaber.pojos.attestation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AttestationPolicy {

    /*
    * Holds the name of the attestation property. eg. education, certificate, course
    *
    * */
    private String property;
    /**
     * Holds the value of the jsonpath
     */
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
    * It will be used as first filter for fetching claims
    * */
    private String attestorEntity;

    public List<String> getPaths() {
        return paths;
    }

    public AttestationType getType() {
        return type;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public void setType(AttestationType type) {
        this.type = type;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    @Override
    public String toString() {
        return "AttestationPolicy{" +
                "property='" + property + '\'' +
                ", paths=" + paths +
                ", type=" + type +
                '}';
    }

    public boolean hasProperty(String property) {
        return getProperty().equals(property);
    }

    public String getAttestorEntity() {
        return attestorEntity;
    }

    public void setAttestorEntity(String attestorEntity) {
        this.attestorEntity = attestorEntity;
    }
}
