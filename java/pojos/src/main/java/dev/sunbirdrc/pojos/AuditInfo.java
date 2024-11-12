package dev.sunbirdrc.pojos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AuditInfo {
    private String op;
    private String path;
    @JsonIgnore
    private Object value;
    @JsonIgnore
    private String from;
}
