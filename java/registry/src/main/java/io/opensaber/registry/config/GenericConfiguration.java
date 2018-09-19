package io.opensaber.registry.config;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import es.weso.schema.Schema;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.registry.authorization.AuthorizationFilter;
import io.opensaber.registry.authorization.KeyCloakServiceImpl;
import io.opensaber.registry.exception.CustomException;
import io.opensaber.registry.exception.CustomExceptionHandler;
import io.opensaber.registry.interceptor.*;
import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.impl.*;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.model.AuditRecord;
import io.opensaber.registry.schema.config.SchemaConfigurator;
import io.opensaber.registry.sink.*;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class GenericConfiguration implements WebMvcConfigurer {

	private static Logger logger = LoggerFactory.getLogger(GenericConfiguration.class);

	@Autowired
	private Environment environment;
	
	@Value("${encryption.service.connection.timeout}")
	private int connectionTimeout;

	@Value("${encryption.service.read.timeout}")
	private int readTimeout;
	
	@Value("${encryption.service.connection.request.timeout}")
	private int connectionRequestTimeout;

	@Value("${authentication.enabled}")
	private boolean authenticationEnabled;

	@Value("${perf.monitoring.enabled}")
	private boolean performanceMonitoringEnabled;
	
	@Value("${registry.system.base}")
	private String registrySystemBase;
	
	@Value("${registry.context.base}")
	private String registryContextBase;
	
	@Value("${signature.schema.config.name}")
	private String signatureSchemaConfigName;

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		return objectMapper;
	}

	@Bean
	public OpenSaberInstrumentation instrumentationStopWatch() {
		return new OpenSaberInstrumentation(performanceMonitoringEnabled);
	}

	@Bean
	public Gson gson(){
		return new Gson();
	}

	@Bean
	public Middleware jsonldConverter(){
		return new JSONLDConverter();
	}

	@Bean
	public Middleware rdfConverter(){
		return new RDFConverter();
	}

    @Bean
    public AuthorizationInterceptor authorizationInterceptor() {
        return new AuthorizationInterceptor(authorizationFilter(), gson());
    }

	@Bean
	public RDFConversionInterceptor rdfConversionInterceptor() {
		return new RDFConversionInterceptor(rdfConverter(), gson());
	}

	@Bean
	public RDFValidationMappingInterceptor rdfValidationMappingInterceptor() {
		return new RDFValidationMappingInterceptor(rdfValidationMapper(), gson());
	}

	@Bean
	public RDFValidationInterceptor rdfValidationInterceptor() {
		return new RDFValidationInterceptor(rdfValidator(), gson());
	}
	
	@Bean
	public SignaturePresenceValidationInterceptor signaturePresenceValidationInterceptor() {
		return new SignaturePresenceValidationInterceptor(signaturePresenceValidator(), gson());
	}

	@Bean
	public Middleware authorizationFilter(){
		return new AuthorizationFilter(new KeyCloakServiceImpl());
	}
	
	@Bean
	public SchemaConfigurator schemaConfiguration() throws IOException, CustomException {
		String fieldConfigFileName = environment.getProperty(Constants.FIELD_CONFIG_SCEHEMA_FILE);
		String validationConfigFileForCreate = environment.getProperty(Constants.SHEX_CREATE_PROPERTY_NAME);
		String validationConfigFileForUpdate = environment.getProperty(Constants.SHEX_UPDATE_PROPERTY_NAME);
		if (fieldConfigFileName == null) {
			throw new CustomException(Constants.SCHEMA_CONFIGURATION_MISSING);
		}
		if (validationConfigFileForCreate == null || validationConfigFileForUpdate == null) {
			throw new CustomException(Constants.VALIDATION_CONFIGURATION_MISSING);
		}

		OpenSaberInstrumentation watch = instrumentationStopWatch();
		watch.start("SchemaConfigurator.initialization");
		SchemaConfigurator schemaConfigurator = new SchemaConfigurator(fieldConfigFileName, validationConfigFileForCreate, validationConfigFileForUpdate, registrySystemBase);
		watch.stop("SchemaConfigurator.initialization");
		return schemaConfigurator ;
	}

	@Bean
	public Middleware rdfValidator() {
		Schema schemaForCreate = null;
		Schema schemaForUpdate = null;
		try {
			schemaForCreate = schemaConfiguration().getSchemaForCreate();
			schemaForUpdate = schemaConfiguration().getSchemaForUpdate();
		} catch (Exception e) {
			logger.error("Unable to retrieve schema for validations");
		}
		return new RDFValidator(schemaForCreate, schemaForUpdate);
	}
	
	@Bean
	public Middleware signaturePresenceValidator() {
		Schema schemaForCreate = null;
		Model schemaConfig = null;
		try {
			schemaForCreate = schemaConfiguration().getSchemaForCreate();
			schemaConfig = schemaConfiguration().getSchemaConfig();
		} catch (Exception e) {
			logger.error("Unable to retrieve schema for signature validations");
		}
		return new SignaturePresenceValidator(schemaForCreate, registryContextBase, registrySystemBase, signatureSchemaConfigName, ((RDFValidator)rdfValidator()).getShapeTypeMap(), schemaConfig);
	}


	@Bean
	@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public AuditRecord auditRecord() {
		return new AuditRecord();
	}

    @Bean
    public RestTemplate restTemaplteProvider() throws IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectTimeout(connectionTimeout);
        requestFactory.setConnectionRequestTimeout(connectionRequestTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return new RestTemplate(requestFactory);
    }

	
	@Bean
	public DatabaseProvider databaseProvider() {
		String dbProvider = environment.getProperty(Constants.DATABASE_PROVIDER);
		DatabaseProvider provider;
		if (dbProvider.equalsIgnoreCase(Constants.GraphDatabaseProvider.ORIENTDB.getName())) {
			provider = new OrientDBGraphProvider(environment);
			provider.initializeGlobalGraphConfiguration();
		} else if (dbProvider.equalsIgnoreCase(Constants.GraphDatabaseProvider.NEO4J.getName())) {
			provider = new Neo4jGraphProvider(environment);
			provider.initializeGlobalGraphConfiguration();
		} else if (dbProvider.equalsIgnoreCase(Constants.GraphDatabaseProvider.SQLG.getName())) {
			provider = new SqlgProvider(environment);
			provider.initializeGlobalGraphConfiguration();
		} else if (dbProvider.equalsIgnoreCase(Constants.GraphDatabaseProvider.TINKERGRAPH.getName())) {
			provider = new TinkerGraphProvider(environment);
			provider.initializeGlobalGraphConfiguration();
		} else if (dbProvider.equalsIgnoreCase(Constants.GraphDatabaseProvider.CASSANDRA.getName())) {
			provider = new JanusGraphStorage(environment);
			provider.initializeGlobalGraphConfiguration();
		} else {
			throw new RuntimeException("No Database Provider is configured. Please configure a Database Provider");
		}

		return provider;
	}

	@Bean
	public UrlValidator urlValidator(){
		return new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
	}

	@Bean
	public Middleware rdfValidationMapper() {
		Model validationConfig = null;
		try{
			validationConfig = schemaConfiguration().getValidationConfig();
		}catch(Exception e){
			logger.error("Unable to get validation configuration");
		}
		return new RDFValidationMapper(validationConfig);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
	    if(authenticationEnabled) {
            registry.addInterceptor(authorizationInterceptor())
                    .addPathPatterns("/**").excludePathPatterns("/health", "/error").order(1);
	    }
		registry.addInterceptor(rdfConversionInterceptor())
				.addPathPatterns("/add", "/update","/search").order(2);
		registry.addInterceptor(rdfValidationInterceptor())
				.addPathPatterns("/add", "/update").order(3);
		registry.addInterceptor(signaturePresenceValidationInterceptor())
		.addPathPatterns("/add", "/update").order(4);
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		try {
			registry.addResourceHandler("/resources/**")
					.addResourceLocations("classpath:vocab/1.0/")
					.setCachePeriod(3600)
					.resourceChain(true)
					.addResolver(new PathResourceResolver());
		} catch (Exception e) {
			throw e;
		}

	}

	@Bean
    public HandlerExceptionResolver customExceptionHandler () {
        return new CustomExceptionHandler(gson());
    }
}
