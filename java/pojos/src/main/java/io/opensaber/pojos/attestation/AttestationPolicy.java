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
     * Holds the info of who can do the attestation
     */
    private List<String> roles;

    public List<String> getPaths() {
        return paths;
    }

    public AttestationType getType() {
        return type;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public void setType(AttestationType type) {
        this.type = type;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public boolean isValidRole(List<String> role) {
        return this.roles.contains(role);
    }

    @Override
    public String toString() {
        return "AttestationPolicy{" +
                "property='" + property + '\'' +
                ", paths=" + paths +
                ", type=" + type +
                ", roles='" + roles + '\'' +
                '}';
    }

    public boolean hasProperty(String property) {
        return getProperty().equals(property);
    }
}
