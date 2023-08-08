package dev.sunbirdrc.claim.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class StudentRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="rollNumber")
    private String rollNumber;

    @Column(name="regNumber")
    private String regNumber;

    @Column(name="dob")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private Date dob;

    @Column(name="email")
    private String email;

}
