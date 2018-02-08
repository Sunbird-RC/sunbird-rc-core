package io.opensaber.registry.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import io.opensaber.registry.interceptor.RDFConversionInterceptor;
import io.opensaber.registry.interceptor.RDFValidationInterceptor;
import io.opensaber.registry.interceptor.RdfToJsonldInterceptor;
import io.opensaber.registry.middleware.impl.RdfToJsonldConverter;


/**
 * 
 * @author jyotsna
 *
 */
@Configuration
public class InterceptorConfiguration extends WebMvcConfigurerAdapter {
	
	@Override 
    public void addInterceptors(InterceptorRegistry registry) { 
		//registry.addInterceptor(new JsonldToRdfInterceptor(new JsonldToRdfConverter())).addPathPatterns("/convertToRdf");
		registry.addInterceptor(new RDFConversionInterceptor()).addPathPatterns("/addEntity");
		registry.addInterceptor(new RDFValidationInterceptor()).addPathPatterns("/addEntity");
		registry.addInterceptor(new RdfToJsonldInterceptor(new RdfToJsonldConverter())).addPathPatterns("/retrieveJsonld");
	}

}
