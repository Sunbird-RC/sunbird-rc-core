package dev.sunbirdrc.plugin.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RequestDto {
    String appId, clientId, secretKey;
}
