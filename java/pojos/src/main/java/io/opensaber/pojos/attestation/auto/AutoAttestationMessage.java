package io.opensaber.pojos.attestation.auto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;

@JsonSerialize
@Getter
@Setter
public class AutoAttestationMessage {
    AutoAttestationPolicy autoAttestationPolicy;
    JsonNode input;
    String accessToken;
    String url;
}
