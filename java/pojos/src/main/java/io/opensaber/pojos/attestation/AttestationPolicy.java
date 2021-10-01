package io.opensaber.pojos.attestation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AttestationPolicy {

    /**
    * name property will be used to pick the specific attestation policy
    * */
    private String name;
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
    /*
    * It will be used to define the actor name
    * */
    private String attestorPlugin;
    /*
    * It will be used for signin redirection eg. consent based screens
    * */
    private String attestorSignin;

    /**
    * nodePath contains the pointer to get the attestation node
    * */
    private String nodePath;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAttestorPlugin() {
        return attestorPlugin;
    }

    public void setAttestorPlugin(String attestorPlugin) {
        this.attestorPlugin = attestorPlugin;
    }

    public String getAttestorSignin() {
        return attestorSignin;
    }

    public void setAttestorSignin(String attestorSignin) {
        this.attestorSignin = attestorSignin;
    }

    public String getNodePath() {
        return nodePath;
    }

    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
    }
}
