package io.opensaber.pojos.attestation.auto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AutoAttestationPolicy {
    private String parentProperty;
    private String property;
    private String nodeRef;
    private String valuePath;
    private String typePath;
}
