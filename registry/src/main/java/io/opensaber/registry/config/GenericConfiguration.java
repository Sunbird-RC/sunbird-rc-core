package io.opensaber.registry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author jyotsna
 *
 */
@Configuration
public class GenericConfiguration {
	
	@Bean
	public ObjectMapper objectMapper() {
	    return new ObjectMapper();
	}

}
