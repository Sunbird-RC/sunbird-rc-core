package io.opensaber.pojos.attestation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

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
    private List<String> properties;
    /*
    * Holds the name of the attestation property. eg. education, certificate, course
    *
    * */
    private Map<String, String> attestationProperties;
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
    * It will be used to define the actor name
    * */
    private String attestorPlugin;
    /*
    * It will be used for signin redirection eg. consent based screens
    * */
    private String attestorSignin;

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

    public List<String> getProperties() {
        return properties;
    }

    public void setProperties(List<String> properties) {
        this.properties = properties;
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
                "property='" + properties + '\'' +
                ", paths=" + paths +
                ", type=" + type +
                '}';
    }

    public boolean hasProperty(String property) {
        return getProperties().equals(property);
    }

    public String getAttestorEntity() {
        String[] split = attestorPlugin.split("entity=");
        return split.length == 2 ? split[1] : "";
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
        return name + "/[]";
    }

    public Map<String, String> getAttestationProperties() {
        return attestationProperties;
    }

    public void setAttestationProperties(Map<String, String> attestationProperties) {
        this.attestationProperties = attestationProperties;
    }
}
