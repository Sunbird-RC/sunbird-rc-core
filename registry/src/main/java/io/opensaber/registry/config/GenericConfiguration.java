package io.opensaber.registry.config;

import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * 
 * @author jyotsna
 *
 */
@Configuration
public class GenericConfiguration extends WebMvcConfigurerAdapter {
	
	@Bean
	public ObjectMapper objectMapper() {
	    ObjectMapper objectMapper = new ObjectMapper();
	    objectMapper.setSerializationInclusion(Include.NON_NULL);
	    return objectMapper;
	}
}
