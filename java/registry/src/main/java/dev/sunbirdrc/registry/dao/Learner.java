package dev.sunbirdrc.registry.dao;

import lombok.Data;

import java.util.List;

@Data
public class Learner {
    private String name;
    private String rollNumber;
    private String enrollmentNumber;
    private List<Credential> credentialsList;

}
