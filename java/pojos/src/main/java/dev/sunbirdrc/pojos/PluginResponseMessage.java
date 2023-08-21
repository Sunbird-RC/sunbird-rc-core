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
import java.util.Map;

@JsonSerialize
@Data
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginResponseMessage {
    private String policyName;
    private String sourceEntity;
    private String sourceOSID;
    private String attestationOSID;
    private String attestorPlugin;
    private String response;
    private String signedData;
    //additional response received:
    private JsonNode additionalData;
    private String status;
    private Date date;
    private Date validUntil;
    private String version;
    private String userId;
    private Map<String, List<String>> propertiesOSID;
    private String emailId;

    private String credType;
    @Builder.Default
    private List<PluginFile> files = new ArrayList<>();
}


