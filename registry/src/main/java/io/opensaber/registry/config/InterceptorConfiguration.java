package io.opensaber.registry.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import io.opensaber.registry.middleware.JsonldToRdfConverter;
import io.opensaber.registry.middleware.RdfToJsonldConverter;


/**
 * 
 * @author jyotsna
 *
 */
@Configuration
public class InterceptorConfiguration extends WebMvcConfigurerAdapter {
	
	@Override 
    public void addInterceptors(InterceptorRegistry registry) { 
		registry.addInterceptor(new JsonldToRdfConverter()).addPathPatterns("/convertToRdf");
		registry.addInterceptor(new RdfToJsonldConverter()).addPathPatterns("/retrieveJsonld");
	}

}
