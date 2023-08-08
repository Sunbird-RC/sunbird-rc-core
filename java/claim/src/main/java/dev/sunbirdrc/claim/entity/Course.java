package dev.sunbirdrc.claim.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="courseName")
    private String courseName;

    @Column(name="collegeName")
    private String collegeName;

    @Column(name="examiningBody")
    private String examiningBody;

    @Column(name="finalYearRollNo")
    private String finalYearRollNo;

    @Column(name="joiningMonth")
    private String joiningMonth;

    @Column(name="joiningYear")
    private String joiningYear;

    @Column(name="passingMonth")
    private String passingMonth;

    @Column(name="passingYear")
    private String passingYear;

    @Column(name="armyRegdNo")
    private String armyRegdNo;

    @Column(name="date")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private Date date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    @JsonIgnore
    private Candidate candidate;

}