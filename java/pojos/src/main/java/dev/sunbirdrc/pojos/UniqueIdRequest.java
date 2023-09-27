package dev.sunbirdrc.pojos;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UniqueIdRequest {

    String idName;
    String tenantId;
    String format;

}
