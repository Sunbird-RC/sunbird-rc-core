package dev.sunbirdrc.claim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManualPendingMailDTO {
    private String outsideEntityMailId;
    private String name;
    private String email;
    private String council;
    private String gender;
    private String examBody;
    private String diplomaNumber;
    private String nursingCollage;
    private String docProof;
    private String courseState;
    private String courseCouncil;
    private String country;
    private String state;
}
