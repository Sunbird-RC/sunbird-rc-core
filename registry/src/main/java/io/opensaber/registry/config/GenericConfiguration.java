package io.opensaber.registry.config;

import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.opensaber.registry.middleware.impl.JsonldToRdfConverter;
import io.opensaber.registry.middleware.impl.RdfToJsonldConverter;

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

	@Bean
	public JsonldToRdfConverter jsonldToRdfConverter(){
		return new JsonldToRdfConverter();
	}
	
	@Bean
	public RdfToJsonldConverter rdfToJsonldConverter(){
		return new RdfToJsonldConverter();
	}
}
