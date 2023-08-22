package dev.sunbirdrc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@AllArgsConstructor
@Data
public class UserOtpDTO {

    @NotBlank(message = "Username is required field")
    @Email(message = "Username accepts only user mail id")
    private String username;

    @NotBlank(message = "OTP is required field")
    @Size(min = 6, max = 6, message = "OTP length should be six digit")
    private String otp;

    @NotBlank(message = "Password is required field")
    private String password;
}
