package dev.sunbirdrc.entity;

import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Builder
@Data
@Entity
@Table(name = "user_details")
public class UserDetails {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private String id;

    @Column
    private String userId;
    @Column
    private String userName;
    @Column
    private String firstName;
    @Column
    private String lastName;
    @Column
    private String email;
    @Column
    private Boolean enabled;
    @Column
    private String rollNo;
    @Column
    private String instituteId;
    @Column
    private String instituteName;
    @Column
    private String phoneNo;
}
