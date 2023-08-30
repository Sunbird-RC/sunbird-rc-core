package dev.sunbirdrc.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@AllArgsConstructor
@Data
public class AdminLoginDTO {
    @NotBlank(message = "Email is required field")
    @Email(message = "Invalid mail id")
    private String email;

    @NotBlank(message = "OTP is required field")
    @Size(min = 6, max = 6, message = "OTP length should be six digit")
    private String otp;
}
