package dev.sunbirdrc.plugin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RequestCredentialsDto {
    String id;
    CredentialsRequestDto request;
    String requesttime;
    String version;
}

