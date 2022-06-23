package dev.sunbirdrc.plugin.dto;


import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class RequestOTPDto {
    String id;
    String individualId;
    String individualIdType;
    Map<String, Object> metadata;
    List<String> otpChannel;
    String requestTime;
    String version;
    String transactionID;
}
