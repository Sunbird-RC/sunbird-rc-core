package dev.sunbirdrc.claim.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@Entity
@EqualsAndHashCode
@Table(name = ClaimNote.TABLE_NAME)
public class ClaimNote {
    public static final String TABLE_NAME = "claim_notes";
    private static final String CREATED_AT = "created_at";

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private String id;
    @Column
    private String entityId;
    @Column
    private String propertyURI;
    @Column
    private String addedBy;
    @Column
    private String notes;
    @Column(name = ClaimNote.CREATED_AT)
    private Date createdAt;

    @Column(columnDefinition = "varchar(255) default 'Default Claim Id'")
    private String claimId;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}
