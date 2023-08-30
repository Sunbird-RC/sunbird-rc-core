package dev.sunbirdrc.claim.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YearsOfCourse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="courseYear")
    private String courseYear;

    @Column (name="examYear")
    private String examYear;

    @Column (name="examMonth")
    private String examMonth;

    @Column (name="result")
    private String result;

    @Column (name="totalMarksObtained")
    private String totalMarksObtained;

    @Column (name="totalMarksObtainedInWord")
    private String totalMarksObtainedInWord;

    @Column (name="maxTotal")
    private String maxTotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_details_id")
    @JsonIgnore
    private StudentDetails studentDetails;


}
