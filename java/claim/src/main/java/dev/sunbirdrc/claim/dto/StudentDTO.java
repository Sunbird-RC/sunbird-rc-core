package dev.sunbirdrc.claim.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {
    @JsonProperty("rollNo")
    private String rollNo;

    @JsonProperty("dob")
    private String dob;
}
