package dev.sunbirdrc.claim.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CertificateMailDto {
    private String name;
    private String certificate;
    private String emailAddress;

    private String credentialsType;

    private String certificateBase64;
}
