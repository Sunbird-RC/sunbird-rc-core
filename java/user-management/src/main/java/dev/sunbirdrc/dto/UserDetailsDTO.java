package dev.sunbirdrc.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@AllArgsConstructor
@Data
@Builder
public class UserDetailsDTO {
    @Email(message = "Username should be valid mail id")
    @NotBlank(message = "userName is required field")
    private String userName;

    @NotBlank(message = "email is required field")
    @Email(message = "Invalid mail id")
    private String email;

    private String password;

    @NotBlank(message = "firstName is required field")
    private String firstName;

    @NotBlank(message = "lastName is required field")
    private String lastName;

    @NotBlank(message = "rollNo is required field")
    private String rollNo;

    @NotBlank(message = "instituteId is required field")
    private String instituteId;

    @NotBlank(message = "instituteName is required field")
    private String instituteName;

    @NotBlank(message = "phoneNo is required field")
    private String phoneNo;

    private String userId;
}
