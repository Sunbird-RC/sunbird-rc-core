package io.opensaber.registry.config;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.sink.Neo4jGraphProvider;
import io.opensaber.registry.sink.OrientDBGraphProvider;
import io.opensaber.registry.sink.SqlgProvider;
import io.opensaber.registry.sink.TinkerGraphProvider;

import org.apache.tomcat.util.http.parser.Authorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import io.opensaber.registry.authorization.AuthorizationFilter;
import io.opensaber.registry.interceptor.AuthorizationInterceptor;
import io.opensaber.registry.interceptor.RDFConversionInterceptor;
import io.opensaber.registry.interceptor.RDFValidationInterceptor;
import io.opensaber.registry.interceptor.RDFValidationMappingInterceptor;
import io.opensaber.registry.middleware.impl.RDFConverter;
import io.opensaber.registry.middleware.impl.RDFValidationMapper;
import io.opensaber.registry.middleware.impl.RDFValidator;
import io.opensaber.registry.middleware.impl.RdfToJsonldConverter;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.schema.config.SchemaConfigurator;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@PropertySource(value = {"classpath:config-${spring.profiles.active}.properties"})
public class GenericConfiguration implements WebMvcConfigurer {

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
	public Gson gson(){
		return new Gson();
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
	public AuthorizationFilter authorizationFilter(){
		return new AuthorizationFilter();
	}

	@Bean
	public RDFValidator rdfValidator(){
		String shexFileName = environment.getProperty(Constants.SHEX_PROPERTY_NAME);
		return new RDFValidator(shexFileName);
	}
	
	@Bean
	public SchemaConfigurator schemaConfiguration() throws IOException{
		String fieldConfigFileName = environment.getProperty(Constants.FIELD_CONFIG_SCEHEMA_FILE);
		return new SchemaConfigurator(fieldConfigFileName);
	}

	@Bean
	public DatabaseProvider databaseProvider() {
		String dbProvider = environment.getProperty(Constants.DATABASE_PROVIDER);
		DatabaseProvider provider;
		if (dbProvider.equalsIgnoreCase(Constants.GraphDatabaseProvider.ORIENTDB.getName())) {
            provider = new OrientDBGraphProvider(environment);
		} else if (dbProvider.equalsIgnoreCase(Constants.GraphDatabaseProvider.NEO4J.getName())) {
            provider = new Neo4jGraphProvider(environment);
		} else if (dbProvider.equalsIgnoreCase(Constants.GraphDatabaseProvider.SQLG.getName())) {
            provider = new SqlgProvider(environment);
		} else if (dbProvider.equalsIgnoreCase(Constants.GraphDatabaseProvider.TINKERGRAPH.getName())) {
            provider = new TinkerGraphProvider(environment);
		} else {
			throw new RuntimeException("No Database Provider is configured. Please configure a Database Provider");
		}
        provider.getGraphStore().variables().set("@persisted",true);
		return provider;
	}

	@Bean
	public RDFValidationMapper rdfValidationMapper(){
		Map<String,String> typeValidationMap = new HashMap<String,String>();
		EnumSet.allOf(Constants.ValidationMapper.class)
		.forEach(type -> {
			String key = environment.getProperty(type.getName()+Constants.SHAPE_TYPE);
			String value = environment.getProperty(type.getName()+Constants.SHAPE_NAME);
			if(key!=null && value!=null){
				typeValidationMap.put(key,value);
			}
		});
		return new RDFValidationMapper(typeValidationMap);
	}

	@Override 
	public void addInterceptors(InterceptorRegistry registry) { 
		registry.addInterceptor(new AuthorizationInterceptor(authorizationFilter(),gson())).addPathPatterns("/**").order(1);
		registry.addInterceptor(new RDFConversionInterceptor(rdfConverter(),gson())).addPathPatterns("/addEntity", "/entity/{id}").order(2);
		registry.addInterceptor(new RDFValidationMappingInterceptor(rdfValidationMapper())).addPathPatterns("/addEntity", "/entity/{id}").order(3);
		registry.addInterceptor(new RDFValidationInterceptor(rdfValidator(),gson())).addPathPatterns("/addEntity", "/entity/{id}").order(4);

	}



}
