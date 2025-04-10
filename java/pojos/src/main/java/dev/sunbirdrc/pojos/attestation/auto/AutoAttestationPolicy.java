package dev.sunbirdrc.pojos.attestation.auto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AutoAttestationPolicy {
    private String parentProperty;
    private String property;
    private String nodeRef;
    private String valuePath;
    private String typePath;
}
