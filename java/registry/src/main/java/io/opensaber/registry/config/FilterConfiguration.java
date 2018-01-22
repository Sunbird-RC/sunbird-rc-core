/*package io.opensaber.registry.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opensaber.registry.filter.JsonldToRdfFilter;
import io.opensaber.registry.filter.RdfToJsonldFilter;

@Configuration
public class FilterConfiguration {
	
	@Bean
	public FilterRegistrationBean jsonldToRdfFilterRegistration(){

		FilterRegistrationBean registration = new FilterRegistrationBean();
		registration.setFilter(new JsonldToRdfFilter());
		registration.addUrlPatterns("/convertToRdf");
		registration.setName("jsonldToRdfFilter");
		registration.setOrder(1);
		return registration;
	}
	
	@Bean
	public FilterRegistrationBean RdfToJsonldFilterRegistration(){

		FilterRegistrationBean registration = new FilterRegistrationBean();
		registration.setFilter(new RdfToJsonldFilter());
		registration.addUrlPatterns("/retrieveJsonld");
		registration.setName("rdfToJsonldFilter");
		registration.setOrder(2);
		return registration;
	}

}
*/