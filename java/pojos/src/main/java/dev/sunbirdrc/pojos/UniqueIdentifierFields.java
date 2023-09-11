package dev.sunbirdrc.pojos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniqueIdentifierFields {

    String field;
    String regularExpression;
    
}
