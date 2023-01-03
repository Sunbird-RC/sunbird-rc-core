package dev.sunbirdrc.registry.model;

import lombok.Data;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotBlank;

@Data
public class OAuth2Resources {
	@NotBlank
	private String uri;
	private OAuth2Properties properties = new OAuth2Properties();

}
