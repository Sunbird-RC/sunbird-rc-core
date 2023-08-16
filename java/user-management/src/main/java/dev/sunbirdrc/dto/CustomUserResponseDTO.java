package dev.sunbirdrc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Data
@Builder
public class CustomUserResponseDTO {
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String roleName;
    private String status;
}
