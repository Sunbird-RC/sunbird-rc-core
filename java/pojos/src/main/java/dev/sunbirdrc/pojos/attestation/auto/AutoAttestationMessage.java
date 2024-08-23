package dev.sunbirdrc.pojos.attestation.auto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonSerialize
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AutoAttestationMessage {
    AutoAttestationPolicy autoAttestationPolicy;
    JsonNode input;
    String accessToken;
    String url;
}
