package io.opensaber.registry.model.attestation;
import java.util.List;

public class AttestationPolicy {

    /**
     * Holds the value of the jsonpath
     */
    private String path;
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
    private List<String> roles;

    public String getPath() {
        return path;
    }

    public AttestationType getType() {
        return type;
    }

    public String getBy() {
        return by;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setType(AttestationType type) {
        this.type = type;
    }

    public void setBy(String by) {
        this.by = by;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
