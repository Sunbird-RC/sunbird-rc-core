package dev.sunbirdrc.registry.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class AttestationStep {
    String apiURL;
    String apiMethod;
    String apiRequestSchema;
}
