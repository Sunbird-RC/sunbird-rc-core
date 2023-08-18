package dev.sunbirdrc.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@AllArgsConstructor
@Data
@Builder
public class UserLoginDTO {
    @NotBlank(message = "Username is required field")
    @Email(message = "Username accepts only user mail id")
    private String username;

    @NotBlank(message = "Password is required field")
    private String password;
}
