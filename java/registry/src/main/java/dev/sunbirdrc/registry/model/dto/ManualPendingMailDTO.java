package dev.sunbirdrc.registry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManualPendingMailDTO {
    private String name;
    private String emailAddress;
    private String council;
    private String itemName;
}
