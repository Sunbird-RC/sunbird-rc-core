package io.opensaber.registry.config;


import java.nio.file.Path;
import java.nio.file.Paths;

import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.sink.Neo4jGraphProvider;
import io.opensaber.registry.sink.OrientDBGraphProvider;
import io.opensaber.registry.sink.SqlgProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.opensaber.registry.interceptor.RDFConversionInterceptor;
import io.opensaber.registry.interceptor.RDFValidationInterceptor;
import io.opensaber.registry.middleware.impl.RDFConverter;
import io.opensaber.registry.middleware.impl.RDFValidator;
import io.opensaber.registry.middleware.impl.RdfToJsonldConverter;
import io.opensaber.registry.middleware.util.Constants;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import scalaz.Const;

/**
 * 
 * @author jyotsna
 *
 */
@Configuration
@PropertySource(value = {"classpath:config-${spring.profiles.active}.properties"})
public class GenericConfiguration extends WebMvcConfigurerAdapter {

	private static Logger logger = LoggerFactory.getLogger(GenericConfiguration.class);

	@Autowired
	private Environment environment;
	
	@Bean
	public ObjectMapper objectMapper() {
	    ObjectMapper objectMapper = new ObjectMapper();
	    objectMapper.setSerializationInclusion(Include.NON_NULL);
	    return objectMapper;
	}
	
	@Bean
	public RdfToJsonldConverter rdfToJsonldConverter(){
		return new RdfToJsonldConverter();
	}
	
	@Bean
	public RDFConverter rdfConverter(){
		return new RDFConverter();
	}
	
	@Bean
	public RDFValidator rdfValidator(){
		String shexFileName = environment.getProperty(Constants.SHEX_PROPERTY_NAME);
		String shexFilePath = this.getClass().getClassLoader().getResource(shexFileName).getPath();
		Path filePath = Paths.get(shexFilePath);
		return new RDFValidator(filePath);
	}

	/*
	@Bean
	public GraphDBFactory graphDBFactory() {
		return new GraphDBFactory(environment);
	}
	*/

	@Bean
	public DatabaseProvider databaseProvider() {
		String dbProvider = environment.getProperty(Constants.DATABASE_PROVIDER);
		if (dbProvider.equalsIgnoreCase(Constants.GraphDatabaseProvider.ORIENTDB.getName())) {
			return new OrientDBGraphProvider(environment);
		} else if (dbProvider.equalsIgnoreCase(Constants.GraphDatabaseProvider.NEO4J.getName())) {
			return new Neo4jGraphProvider(environment);
		} else if (dbProvider.equalsIgnoreCase(Constants.GraphDatabaseProvider.SQLG.getName())) {
			return new SqlgProvider(environment);
		} else {
			throw new RuntimeException("No Database Provider is configured. Please configure a Database Provider");
		}
	}
	
	@Override 
    public void addInterceptors(InterceptorRegistry registry) { 
		//registry.addInterceptor(new JsonldToRdfInterceptor(new JsonldToRdfConverter())).addPathPatterns("/convertToRdf");
		registry.addInterceptor(new RDFConversionInterceptor(rdfConverter())).addPathPatterns("/addEntity");
		registry.addInterceptor(new RDFValidationInterceptor(rdfValidator())).addPathPatterns("/addEntity");
		
	}
	
}
