package dev.sunbirdrc.registry.authorization.pojos;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class OAuth2Resources {
	@NotBlank
	private String uri;
	private OAuth2Properties properties = new OAuth2Properties();

}
