package dev.sunbirdrc.plugin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FetchCredentialsDto {
    String otp;
    String uid;
    String transactionId;
    String osid;
    String attestationOsid;
}
