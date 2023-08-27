package dev.sunbirdrc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@AllArgsConstructor
@Data
public class CustomUserDeleteDTO {
    @NotBlank(message = "Username is required field")
    @Email(message = "Username should be valid mail id")
    private String email;

    private String description;
}