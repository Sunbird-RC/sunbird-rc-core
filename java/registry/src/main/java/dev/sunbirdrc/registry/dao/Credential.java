package dev.sunbirdrc.registry.dao;

import lombok.Data;

@Data
public class Credential {
    private String course;
    private String credentialName;
    private String credentialURL;
    private String issueDate;
}
