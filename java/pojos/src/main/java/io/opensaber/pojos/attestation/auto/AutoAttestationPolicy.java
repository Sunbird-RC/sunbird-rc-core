package io.opensaber.pojos.attestation.auto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AutoAttestationPolicy {
    private String property;
    private String valuePath;
    private String typePath;
}
