package dev.sunbirdrc.claim.entity;


import dev.sunbirdrc.claim.model.ClaimStatus;
import dev.sunbirdrc.pojos.dto.ClaimDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
//@Entity//(name = "\"V_Regulator\"")
//@Table(name = "\"V_Regulator\"")
public class Regulator {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private String ID;
    @Column
    private String name;
    @Column
    private String phoneNumber;
    @Column
    private String council;
    @Column
    private String email;
    @Column
    private String osOwner;
    @Column
    private String osid;
}