package dev.sunbirdrc.claim.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@AllArgsConstructor
@NoArgsConstructor
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