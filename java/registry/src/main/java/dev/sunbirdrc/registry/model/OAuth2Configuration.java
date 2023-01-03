package dev.sunbirdrc.registry.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import java.util.List;

@Validated
@Configuration
@ConfigurationProperties(prefix = "oauth2")
@Component
@Data
public class OAuth2Configuration {
	List<OAuth2Resources> resources;

}
