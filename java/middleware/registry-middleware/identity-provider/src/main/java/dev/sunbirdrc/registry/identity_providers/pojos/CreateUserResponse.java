package dev.sunbirdrc.registry.identity_providers.pojos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserResponse {
	private String userId;
	private String status;
	private String message;

}
