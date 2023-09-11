package dev.sunbirdrc.pojos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniqueIdRequest {

    String idName;
    String tenantId;
    String format;

}
