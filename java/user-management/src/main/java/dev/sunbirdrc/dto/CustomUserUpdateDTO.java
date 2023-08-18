package dev.sunbirdrc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.util.List;

@AllArgsConstructor
@Data
@Builder
public class CustomUserUpdateDTO {
    @Email(message = "Username should be valid mail id")
    @NotBlank(message = "userName is required field")
    private String username;

    @NotBlank(message = "firstName is required field")
    private String firstName;

    @NotBlank(message = "lastName is required field")
    private String lastName;

    private List<String> roleNames;
}
