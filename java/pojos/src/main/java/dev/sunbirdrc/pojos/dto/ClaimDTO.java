package dev.sunbirdrc.pojos.dto;


public class ClaimDTO {
    private String entity;
    private String entityId;
    private String propertyURI;
    private String notes;
    private String status;
    private String conditions;
    private String attestorEntity;
    private String requestorName;
    private String propertyData;
    private String attestationId;
    private String attestationName;

    public String getCredtype() {
        return credtype;
    }

    public void setCredtype(String credtype) {
        this.credtype = credtype;
    }

    private String credtype;

    public String getRequestorName() {
        return requestorName;
    }

    public void setRequestorName(String requestorName) {
        this.requestorName = requestorName;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getPropertyURI() {
        return propertyURI;
    }

    public void setPropertyURI(String propertyURI) {
        this.propertyURI = propertyURI;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public String getAttestorEntity() {
        return attestorEntity;
    }

    public void setAttestorEntity(String attestorEntity) {
        this.attestorEntity = attestorEntity;
    }

    public String getPropertyData() {
        return propertyData;
    }

    public void setPropertyData(String propertyData) {
        this.propertyData = propertyData;
    }

    public String getAttestationId() {
        return attestationId;
    }

    public void setAttestationId(String attestationId) {
        this.attestationId = attestationId;
    }

    public String getAttestationName() {
        return attestationName;
    }

    public void setAttestationName(String attestationName) {
        this.attestationName = attestationName;
    }
}
