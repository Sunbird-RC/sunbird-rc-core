package io.opensaber.claim.entity;


import io.opensaber.claim.model.ClaimStatus;
import io.opensaber.pojos.dto.ClaimDTO;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = Claim.TABLE_NAME)
public class Claim {
    public static final String TABLE_NAME= "claims";
    public static final String CREATED_AT = "created_at";
    private static final String ATTESTED_ON = "attested_on";

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private String id;
    @Column
    private String entity;
    @Column
    private String entityId;
    @Column
    private String property;
    @Column
    private String propertyId;
    @Column(name=Claim.CREATED_AT)
    private Date createdAt;
    @Column(name=Claim.ATTESTED_ON)
    private Date attestedOn;
    @Column
    private String notes;
    @Column
    private String status;
    @Column
    private String conditions;
    @Column
    private String attestorEntity;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
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

    public Date getAttestedOn() {
        return attestedOn;
    }

    public void setAttestedOn(Date attestedOn) {
        this.attestedOn = attestedOn;
    }

    @Override
    public String toString() {
        return "Claim{" +
                "entity='" + entity + '\'' +
                ", entityId='" + entityId + '\'' +
                ", property='" + property + '\'' +
                ", propertyId='" + propertyId + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    public boolean isClosed() {
        return status != null && status.equals(ClaimStatus.CLOSED.name());
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String referenceId) {
        this.conditions = referenceId;
    }

    public String getAttestorEntity() {
        return attestorEntity;
    }

    public void setAttestorEntity(String attestorEntity) {
        this.attestorEntity = attestorEntity;
    }

    public static Claim fromDTO(ClaimDTO claimDTO) {
        Claim claim = new Claim();
        claim.setPropertyId(claimDTO.getPropertyId());
        claim.setProperty(claimDTO.getProperty());
        claim.setEntity(claimDTO.getEntity());
        claim.setEntityId(claimDTO.getEntityId());
        claim.setPropertyId(claimDTO.getPropertyId());
        claim.setConditions(claimDTO.getConditions());
        claim.setAttestorEntity(claimDTO.getAttestorEntity());
        claim.setStatus(ClaimStatus.OPEN.name());
        return claim;
    }
}
