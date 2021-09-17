package io.opensaber.pojos;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

@JsonSerialize
@Data
@RequiredArgsConstructor
public class PluginResponseMessage {
    String policyName;
    String sourceEntity;
    String sourceOSID;
    String attestationOSID;
    String attestorPlugin;
    String signedData;
    //additional response received:
    Map additionalData;
    String status;
    Date date;
    Date validUntil;
    String version;
}
