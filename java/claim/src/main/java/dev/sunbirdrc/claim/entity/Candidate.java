package dev.sunbirdrc.claim.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import dev.sunbirdrc.claim.status.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="title")
    private String title;

    @Column(name="firstName")
    private String firstName;

    @Column(name="middleName")
    private String middleName;

    @Column(name="lastName")
    private String lastName;

    @Column(name="motherName")
    private String motherName;

    @Column(name="fatherName")
    private String fatherName;

    @Column(name="dob")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private Date dob;

    @Column(name="gender")
    private String gender;

    @Column(name="address")
    private String address;

    @Column(name="district")
    private String district;

    @Column(name="state")
    private String state;

    @Column(name="country")
    private String country;

    @Column(name="pinCode")
    private String pinCode;

    @Column(name="mobileNumber")
    private String mobileNumber;

    @Column(name="emailId")
    private String emailId;

    @Column(name="aadharNumber")
    private String aadharNumber;

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Course> courses;

    @Column(name="status")
    @Enumerated(EnumType.STRING)
    private Status status;

}
