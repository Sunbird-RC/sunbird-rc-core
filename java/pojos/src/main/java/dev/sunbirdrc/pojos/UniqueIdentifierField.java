package dev.sunbirdrc.pojos;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UniqueIdentifierField {
    String field;
    String idName;
    String format;
}