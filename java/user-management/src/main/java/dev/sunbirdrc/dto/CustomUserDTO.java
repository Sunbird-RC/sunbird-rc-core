package dev.sunbirdrc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@AllArgsConstructor
@Data
@Builder
public class CustomUserDTO {
    @Email(message = "Username should be valid mail id")
    @NotBlank(message = "userName is required field")
    private String username;

    @NotBlank(message = "email is required field")
    @Email(message = "Invalid mail id")
    private String email;

    @NotBlank(message = "password is required field")
    private String password;

    @NotBlank(message = "firstName is required field")
    private String firstName;

    @NotBlank(message = "lastName is required field")
    private String lastName;

    @NotBlank(message = "roleName is required field")
    private String roleName;
}
