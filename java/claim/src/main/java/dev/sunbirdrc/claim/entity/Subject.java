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
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="name")
    private String name;

    @Column (name="maxExt")
    private String maxExt;

    @Column (name="maxInt")
    private String maxInt;

    @Column (name="obtExt")
    private String obtExt;

    @Column (name="obtInt")
    private String obtInt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_details_id")
    @JsonIgnore
    private StudentDetails studentDetails;
}
