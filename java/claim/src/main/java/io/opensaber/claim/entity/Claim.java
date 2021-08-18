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
    private String propertyURI;
    @Column(name = Claim.CREATED_AT)
    private Date createdAt;
    @Column(name=Claim.ATTESTED_ON)
    private Date attestedOn;
    @Column
    private String status;
    @Column
    private String conditions;
    @Column
    private String attestorEntity;
    @Column
    private String requestorName;

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

    public String getPropertyURI() {
        return propertyURI;
    }

    public void setPropertyURI(String propertyURI) {
        this.propertyURI = propertyURI;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
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
                ", propertyURI='" + propertyURI + '\'' +
                ", status='" + status + '\'' +
                ", condition='" + conditions + '\'' +
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
        claim.setPropertyURI(claimDTO.getPropertyURI());
        claim.setEntity(claimDTO.getEntity());
        claim.setEntityId(claimDTO.getEntityId());
        claim.setConditions(claimDTO.getConditions());
        claim.setAttestorEntity(claimDTO.getAttestorEntity());
        claim.setStatus(ClaimStatus.OPEN.name());
        claim.setRequestorName(claimDTO.getRequestorName());
        return claim;
    }

    public String getRequestorName() {
        return requestorName;
    }

    public void setRequestorName(String requesterName) {
        this.requestorName = requesterName;
    }
}
