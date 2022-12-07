package dev.sunbirdrc.consent.entity;

import dev.sunbirdrc.pojos.dto.ConsentDTO;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

@Entity
@Table(name="consent")
@Getter
@Setter
public class Consent {
    public static final String TABLE_NAME= "claims";
    public static final String CREATED_AT = "created_at";
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private String id;
    @Column
    private String entityName;

    @Column
    private String osOwner;
    @Column
    private String entityId;
    @Column(name = Consent.CREATED_AT)
    private Date createdAt;
    private Date updatedAt;
    @Column
    private boolean status;
    @Column
    private String requestorName;
    @Column
    private String requestorId;
    @Column
    private String expirationTime;
    @ElementCollection
    @MapKeyColumn(name="name")
    @Column(name="value")
    @CollectionTable(name="consent_fields", joinColumns=@JoinColumn(name="consent_id"))
    private Map<String, String> consentFields;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
        status = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    public static Consent fromDTO(ConsentDTO consentDTO) {
        Consent consent = new Consent();
        consent.entityName = consentDTO.getEntityName();
        consent.entityId = consentDTO.getEntityId();
        consent.requestorName = consentDTO.getRequestorName();
        consent.requestorId = consentDTO.getRequestorId();
        consent.consentFields = consentDTO.getConsentFieldsPath();
        consent.expirationTime = consentDTO.getConsentExpiryTime();
        consent.osOwner = consentDTO.getOsOwner().stream().collect(Collectors.joining(","));
        return consent;
    }
}
