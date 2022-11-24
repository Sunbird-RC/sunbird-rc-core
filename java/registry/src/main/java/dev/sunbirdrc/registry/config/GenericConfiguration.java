package dev.sunbirdrc.registry.config;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import dev.sunbirdrc.actors.services.NotificationService;
import dev.sunbirdrc.elastic.ElasticServiceImpl;
import dev.sunbirdrc.elastic.IElasticService;
import dev.sunbirdrc.pojos.AuditRecord;
import dev.sunbirdrc.pojos.Response;
import dev.sunbirdrc.pojos.SunbirdRCInstrumentation;
import dev.sunbirdrc.registry.exception.CustomException;
import dev.sunbirdrc.registry.exception.CustomExceptionHandler;
import dev.sunbirdrc.registry.frame.FrameContext;
import dev.sunbirdrc.registry.interceptor.RequestIdValidationInterceptor;
import dev.sunbirdrc.registry.interceptor.ValidationInterceptor;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.middleware.util.Constants.SchemaType;
import dev.sunbirdrc.registry.model.DBConnectionInfoMgr;
import dev.sunbirdrc.registry.service.IReadService;
import dev.sunbirdrc.registry.service.ISearchService;
import dev.sunbirdrc.registry.service.RegistryService;
import dev.sunbirdrc.registry.service.impl.RegistryServiceImpl;
import dev.sunbirdrc.registry.sink.shard.DefaultShardAdvisor;
import dev.sunbirdrc.registry.sink.shard.IShardAdvisor;
import dev.sunbirdrc.registry.sink.shard.ShardAdvisor;
import dev.sunbirdrc.registry.transform.*;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import dev.sunbirdrc.registry.util.ServiceProvider;
import dev.sunbirdrc.validators.IValidate;
import dev.sunbirdrc.validators.ValidationFilter;
import dev.sunbirdrc.validators.json.jsonschema.JsonValidationServiceImpl;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.sunbird.akka.core.SunbirdActorFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRetry
@EnableAsync
public class GenericConfiguration implements WebMvcConfigurer {

	private static final String DOMAIN = "sunbirdrc.dev";
	private static final String CREATOR = "sunbirdrc";
	private static final String NONCE = "";
	private static final Logger logger = LoggerFactory.getLogger(GenericConfiguration.class);

	static {
		Config config = ConfigFactory.parseResources("sunbirdrc-actors.conf");

		SunbirdActorFactory sunbirdActorFactory = new SunbirdActorFactory(config, "dev.sunbirdrc.actors");
		sunbirdActorFactory.init("sunbirdrc-actors");
	}

	private final String NONE_STR = "none";

	@Autowired
	private IDefinitionsManager iDefinitionsManager;
	@Value("${service.connection.timeout}")
	private int connectionTimeout;
	@Value("${service.read.timeout}")
	private int readTimeout;
	@Value("${service.connection.request.timeout}")
	private int connectionRequestTimeout;
	@Value("${authentication.enabled}")
	private boolean authenticationEnabled;
	@Value(("${authentication.url}"))
	private String authUrl;
	@Value(("${authentication.realm}"))
	private String authRealm;
	@Value(("${authentication.publicKey}"))
	private String authPublicKey;
	@Value("${perf.monitoring.enabled}")
	private boolean performanceMonitoringEnabled;
	@Value("${registry.context.base}")
	private String registryContextBase;
	@Value("${frame.file}")
	private String frameFile;
	@Value("${validation.type}")
	private String validationType = "json";
	@Value("${validation.enabled}")
	private boolean validationEnabled = true;
	@Value("${taskExecutor.index.threadPoolName}")
	private String indexThreadName;
	@Value("${taskExecutor.index.corePoolSize}")
	private int indexCorePoolSize;
	@Value("${taskExecutor.index.maxPoolSize}")
	private int indexMaxPoolSize;
	@Value("${taskExecutor.index.queueCapacity}")
	private int indexQueueCapacity;
	@Value("${auditTaskExecutor.threadPoolName}")
	private String auditThreadName;
	@Value("${auditTaskExecutor.corePoolSize}")
	private int auditCorePoolSize;
	@Value("${auditTaskExecutor.maxPoolSize}")
	private int auditMaxPoolSize;
	@Value("${auditTaskExecutor.queueCapacity}")
	private int auditQueueCapacity;
	@Value("${elastic.search.connection_url}")
	private String elasticConnInfo;
	@Value("${notification.service.connection_url}")
	private String notificationServiceConnInfo;
	@Value("${notification.service.enabled}")
	private boolean notificationServiceEnabled;
	@Value("${notification.service.health_url}")
	private String notificationServiceHealthUrl;
	@Value("${search.providerName}")
	private String searchProviderName;
	@Value("${read.providerName}")
	private String readProviderName;
	@Value("${server.port}")
	private long serverPort;
	@Value("${registry.schema.url}")
	private String schemaUrl;
	@Value("${httpConnection.maxConnections:5}")
	private int httpMaxConnections;
	@Autowired
	private DBConnectionInfoMgr dbConnectionInfoMgr;

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		return objectMapper;
	}

	@Bean
	public SunbirdRCInstrumentation instrumentationStopWatch() {
		return new SunbirdRCInstrumentation(performanceMonitoringEnabled);
	}

	@Bean
	public Gson gson() {
		return new Gson();
	}

	@Bean
	public FrameContext frameContext() {
		return new FrameContext(frameFile, registryContextBase);
	}

	/**
	 * Gets the type of validation configured in the application.yml
	 *
	 * @return
	 * @throws IllegalArgumentException when value is not in known SchemaType enum
	 */
	@Bean
	public SchemaType getValidationType() throws IllegalArgumentException {
		String validationMechanism = validationType.toUpperCase();
		SchemaType st = SchemaType.valueOf(validationMechanism);

		return st;
	}

	@Bean
	public Json2LdTransformer json2LdTransformer() {
		String domain = frameContext().getDomain();
		return new Json2LdTransformer(frameContext().getContent(), domain);
	}

	@Bean
	public Ld2JsonTransformer ld2JsonTransformer() {
		return new Ld2JsonTransformer();
	}

	@Bean
	public Transformer transformer() {
		return new Transformer();
	}

	@Bean
	public Json2JsonTransformer json2JsonTransformer() {
		return new Json2JsonTransformer();
	}

	@Bean
	public ConfigurationHelper configurationHelper() {
		return new ConfigurationHelper();
	}

/*	@Bean
	public AuthorizationInterceptor authorizationInterceptor() {
		return new AuthorizationInterceptor(authorizationFilter());
	}*/

	@Bean
	public RequestIdValidationInterceptor requestIdValidationInterceptor() {
		return new RequestIdValidationInterceptor(requestIdMap());
	}

	@Bean
	public ValidationInterceptor validationInterceptor() throws IOException, CustomException {
		return new ValidationInterceptor(new ValidationFilter(validationServiceImpl()));
	}

	/*
		@Bean
		public Middleware authorizationFilter() {
			return new AuthorizationFilter(new KeyCloakServiceImpl(authUrl, authRealm, authPublicKey));
		}
		*/
	@Bean
	public IValidate validationServiceImpl() throws IOException, CustomException {
		// depends on input type,we need to implement validation
		if (getValidationType() == SchemaType.JSON) {
			IValidate validator = new JsonValidationServiceImpl(schemaUrl);
			iDefinitionsManager.getAllDefinitions().forEach(definition -> {
				logger.debug("Definition: title-" + definition.getTitle() + " , content-" + definition.getContent());
				validator.addDefinitions(definition.getTitle(), definition.getContent());
			});
			logger.info(iDefinitionsManager.getAllDefinitions().size() + " definitions added to validator service ");
			return validator;
		} else {
			logger.error("Fatal - not a known validator mentioned in the application configuration.");
		}
		return null;
	}

	@Bean
	@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
	public AuditRecord auditRecord() {
		return new AuditRecord();
	}

	@Bean
	public RestTemplate restTemplateProvider() throws IOException {
		HttpClient httpClient = HttpClientBuilder.create().setMaxConnPerRoute(httpMaxConnections).setMaxConnTotal(httpMaxConnections * 2).build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		requestFactory.setConnectTimeout(connectionTimeout);
		requestFactory.setConnectionRequestTimeout(connectionRequestTimeout);
		requestFactory.setReadTimeout(readTimeout);
		return new RestTemplate(requestFactory);
	}

//	@Bean
//	public DBProviderFactory dbProviderFactory() {
//		return new DBProviderFactory();
//	}

	@Bean
	public IShardAdvisor shardAdvisor() {
		ShardAdvisor shardAdvisor = new ShardAdvisor();
		if (dbConnectionInfoMgr.getShardProperty().equals(NONE_STR)) {
			return shardAdvisor.getInstance(DefaultShardAdvisor.class.getName());
		} else {
			return shardAdvisor.getInstance(dbConnectionInfoMgr.getShardAdvisorClassName());
		}
	}

	@Bean
	public ISearchService searchService() {
		ServiceProvider searchProvider = new ServiceProvider();
		return searchProvider.getSearchInstance(searchProviderName, isElasticSearchEnabled());
	}

	/**
	 * This method creates read provider implementation bean
	 *
	 * @return
	 */
	@Bean
	public IReadService readService() {
		ServiceProvider searchProvider = new ServiceProvider();
		return searchProvider.getReadInstance(readProviderName, isElasticSearchEnabled());
	}

	@Bean
	public boolean isElasticSearchEnabled() {
		return (searchProviderName.equals("dev.sunbirdrc.registry.service.ElasticSearchService"));
	}

	@Bean
	public UrlValidator urlValidator() {
		return new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
	}

	/**
	 * This method create a Map of request endpoints with request id
	 *
	 * @return Map
	 */
	@Bean
	public Map<String, String> requestIdMap() {
		Map<String, String> requestIdMap = new HashMap<>();
		requestIdMap.put(Constants.REGISTRY_ADD_ENDPOINT, Response.API_ID.CREATE.getId());
		requestIdMap.put(Constants.REGISTRY_READ_ENDPOINT, Response.API_ID.READ.getId());
		requestIdMap.put(Constants.REGISTRY_SEARCH_ENDPOINT, Response.API_ID.SEARCH.getId());
		requestIdMap.put(Constants.REGISTRY_UPDATE_ENDPOINT, Response.API_ID.UPDATE.getId());
		requestIdMap.put(Constants.SIGNATURE_SIGN_ENDPOINT, Response.API_ID.SIGN.getId());
		requestIdMap.put(Constants.SIGNATURE_VERIFY_ENDPOINT, Response.API_ID.VERIFY.getId());
		requestIdMap.put(Constants.REGISTRY_AUDT_READ_ENDPOINT, Response.API_ID.AUDIT.getId());
		return requestIdMap;
	}

	/**
	 * This method attaches the required interceptors. The flags that control
	 * the attachment are read from application configuration.
	 *
	 * @param registry
	 */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		int orderIdx = 1;
		Map<String, String> requestMap = requestIdMap();

		// Verifying our API identifiers and populating the APIMessage bean
		// Do not remove this.
		registry.addInterceptor(requestIdValidationInterceptor()).addPathPatterns(new ArrayList(requestMap.keySet())).order(orderIdx++);

		// Authenticate and authorization check
/*		if (authenticationEnabled) {
			registry.addInterceptor(authorizationInterceptor()).addPathPatterns("/**")
					.excludePathPatterns("/health", "/error", "/_schemas/**").order(orderIdx++);
		}*/

		// Validate the input against the defined schema
		if (validationEnabled) {
			try {
				registry.addInterceptor(validationInterceptor()).addPathPatterns("/add").order(orderIdx++);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (CustomException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		try {
			registry.addResourceHandler("/resources/**").addResourceLocations("classpath:vocab/1.0/").setCachePeriod(3600).resourceChain(true).addResolver(new PathResourceResolver());
		} catch (Exception e) {
			throw e;
		}

	}

	@Bean
	public HandlerExceptionResolver customExceptionHandler() {
		return new CustomExceptionHandler(gson());
	}

	/**
	 * This method creates ThreadPool task-executor
	 *
	 * @return - TaskExecutor
	 */
	@Bean(name = "taskExecutor")
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(indexCorePoolSize);
		executor.setMaxPoolSize(indexMaxPoolSize);
		executor.setQueueCapacity(indexQueueCapacity);
		executor.setThreadNamePrefix(indexThreadName);
		executor.initialize();
		return executor;
	}

	/**
	 * This method creates ThreadPool task-executor for audit
	 *
	 * @return - TaskExecutor
	 */
	@Bean(name = "auditExecutor")
	public TaskExecutor auditTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(auditCorePoolSize);
		executor.setMaxPoolSize(auditMaxPoolSize);
		executor.setQueueCapacity(auditQueueCapacity);
		executor.setThreadNamePrefix(auditThreadName);
		executor.initialize();
		return executor;
	}

	/**
	 * creates elastic-service bean and instanstiates the indices
	 *
	 * @return - IElasticService
	 * @throws IOException
	 */
	@Bean
	public IElasticService elasticService() throws IOException {
		ElasticServiceImpl elasticService = new ElasticServiceImpl();

		if (isElasticSearchEnabled()) {
			elasticService.setType(Constants.ES_DOC_TYPE);
			elasticService.setConnectionInfo(elasticConnInfo);
			elasticService.init(iDefinitionsManager.getAllKnownDefinitions());
		}
		return elasticService;
	}

	@Bean
	public NotificationService notificationService() {
		return new NotificationService(notificationServiceConnInfo, notificationServiceHealthUrl, notificationServiceEnabled);
	}

	@Bean
	public RegistryService registryService() {
		return new RegistryServiceImpl();
	}
//	/** creates elastic-service bean and instanstiates the indices
//	 * @return - IElasticService
//	 * @throws IOException
//	 */
//	@Bean
//	public IAuditService auditService() throws IOException {
//		IAuditService auditService = new AuditProviderFactory().getAuditService(auditFrameStore);
//		return auditService;
//	}
}
