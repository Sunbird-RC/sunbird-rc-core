package dev.sunbirdrc.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.*;

@AllArgsConstructor
@Data
public class AdminDTO {
    @NotBlank(message = "Username is required field")
    @Email(message = "Username should be valid mail id")
    private String username;

    private String description;
}
