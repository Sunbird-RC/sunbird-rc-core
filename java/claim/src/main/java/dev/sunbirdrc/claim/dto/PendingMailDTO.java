package dev.sunbirdrc.claim.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PendingMailDTO {
    private String name;
    private String emailAddress;
    private String council;
    private String itemName;
    private String refNo;
    private String regulatorName;
    private String regulatorEmail;
    private String credType;
    private String registrationNumber;
    private String verifyLink;
    private String nurseRegNo;
    private String registrationType;
    private String courseName;
    private String workPlace;
    private String examBody;

}
