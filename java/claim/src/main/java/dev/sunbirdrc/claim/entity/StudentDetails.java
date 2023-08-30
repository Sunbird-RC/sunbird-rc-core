package dev.sunbirdrc.claim.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (name="name")
    private String name;

    @Column (name="fatherName")
    private String fatherName;

    @Column (name="motherName")
    private String motherName;

    @Column(unique=true)
    private String rollNumber;

    @Column(unique=true)
    private String regNumber;

    @Column (name="course")
    private String course;

    @Column (name="trainingCenter")
    private String trainingCenter;

    @Column (name="trainingPeriod")
    private String trainingPeriod;

    @Column (name="examBody")
    private String examBody;

    @Column (name="examYear")
    private String examYear;

    @Column (name="examMonth")
    private String examMonth;

    @Column (name="enrollNo")
    private String enrollNo;

    @Column (name="orgLogo")
    private String orgLogo;

    @Column (name="barCode")
    private String barcode;

    @Column (name="candidatePic")
    private String candidatePic;

    @Column (name="signaturePic")
    private String signaturePic;

    @Column (name="trainingCenterCode")
    private String trainingCenterCode;

    @Column (name="dated")
    private String dated;

    @Column (name="trainingTitle")
    private String trainingTitle;

    @Column (name="trainingCode")
    private String trainingCode;

    @Column (name="credType")
    private String credType;

    @OneToMany(mappedBy = "studentDetails", cascade = CascadeType.ALL)
    private List<YearsOfCourse> yearsOfCourseList;

    @OneToMany(mappedBy = "studentDetails", cascade = CascadeType.ALL)
    private List<Subject> subjectList;

}