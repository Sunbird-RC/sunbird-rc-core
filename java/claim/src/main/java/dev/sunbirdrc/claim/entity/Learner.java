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
@Table(name = "learner")
public class Learner {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "rollNumber")
    private String rollNumber;

    @Column(name = "registrationNumber")
    private String registrationNumber;

    @OneToMany(mappedBy = "learner", cascade = CascadeType.ALL ,fetch = FetchType.LAZY)
    private List<Credentials> credentialsList;
}

