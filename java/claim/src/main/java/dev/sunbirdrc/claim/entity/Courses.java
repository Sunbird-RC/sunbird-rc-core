package dev.sunbirdrc.claim.entity;

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
public class Courses {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category")
    private String category;
    @Column(name = "courseName")
    private String courseName;

    @Column(name = "courseNameKey")
    private String courseNameKey;

    @Column(name = "shortName")
    private String shortName;
}
