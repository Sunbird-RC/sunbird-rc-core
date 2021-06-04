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
    /**
     * Holds the info of routing
     */
    private String by;
    /**
     * Holds the info of who can do the attestation
     */
    private String role;

    public List<String> getPaths() {
        return paths;
    }

    public AttestationType getType() {
        return type;
    }

    public String getBy() {
        return by;
    }

    public String getRole() {
        return role;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public void setType(AttestationType type) {
        this.type = type;
    }

    public void setBy(String by) {
        this.by = by;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public boolean isValidRole(String role) {
        return this.role.equals(role);
    }

    @Override
    public String toString() {
        return "AttestationPolicy{" +
                "property='" + property + '\'' +
                ", paths=" + paths +
                ", type=" + type +
                ", by='" + by + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
