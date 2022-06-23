package dev.sunbirdrc.plugin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class CredentialsRequestDto {
    Map additionalData;
    @Builder.Default
    String credentialType = "euin";
    @Builder.Default
    String encrypt = "false";
    @Builder.Default
    String encryptionKey = "";
    String individualId, issuer, otp, transactionID, user;
}
