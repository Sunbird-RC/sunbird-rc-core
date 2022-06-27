package dev.sunbirdrc.plugin.dto;


import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AuthRequestDto {
    String id;
    RequestDto request;
    String requesttime;
    String version;
}
