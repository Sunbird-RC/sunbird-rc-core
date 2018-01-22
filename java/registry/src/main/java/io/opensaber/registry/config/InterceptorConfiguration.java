package io.opensaber.registry.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import io.opensaber.registry.middleware.impl.JsonldToRdfConverter;
import io.opensaber.registry.middleware.impl.RdfToJsonldConverter;
import io.opensaber.registry.middleware.interceptor.JsonldToRdfInterceptor;
import io.opensaber.registry.middleware.interceptor.RdfToJsonldInterceptor;


/**
 * 
 * @author jyotsna
 *
 */
@Configuration
public class InterceptorConfiguration extends WebMvcConfigurerAdapter {
	
	@Override 
    public void addInterceptors(InterceptorRegistry registry) { 
		registry.addInterceptor(new JsonldToRdfInterceptor(new JsonldToRdfConverter())).addPathPatterns("/convertToRdf");
		registry.addInterceptor(new RdfToJsonldInterceptor(new RdfToJsonldConverter())).addPathPatterns("/retrieveJsonld");
	}

}
