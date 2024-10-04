package dev.sunbirdrc.pojos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class Entity {
    private Object claim;
    private String signatureValue;
    private Integer keyId;
}
