package dev.sunbirdrc.pojos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@JsonSerialize
@Data
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginResponseMessage {
    String policyName;
    String sourceEntity;
    String sourceOSID;
    String attestationOSID;
    String attestorPlugin;
    String response;
    String signedData;
    //additional response received:
    JsonNode additionalData;
    String status;
    Date date;
    Date validUntil;
    String version;
    @Builder.Default
    List<PluginFile> files = new ArrayList<>();
}


