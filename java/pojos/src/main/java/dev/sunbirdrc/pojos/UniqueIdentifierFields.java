package dev.sunbirdrc.pojos;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UniqueIdentifierFields {

    String field;
    String regularExpression;
    
}
