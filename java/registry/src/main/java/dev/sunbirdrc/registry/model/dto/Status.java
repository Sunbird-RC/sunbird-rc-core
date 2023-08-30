package dev.sunbirdrc.registry.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class Status implements Serializable {

    String mailStatus;
    String certStatus;
    String certUrl;
}
