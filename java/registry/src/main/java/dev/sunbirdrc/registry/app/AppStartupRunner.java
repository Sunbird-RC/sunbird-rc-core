package dev.sunbirdrc.registry.app;

import dev.sunbirdrc.registry.service.CredentialSchemaService;
import dev.sunbirdrc.registry.service.impl.SignatureV2ServiceImpl;
import dev.sunbirdrc.registry.util.EntityParenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AppStartupRunner implements ApplicationRunner {

    private static Logger logger = LoggerFactory.getLogger(AppStartupRunner.class);

	@Autowired
	EntityParenter entityParenter;

	@Value("${signature.enabled}")
	private boolean signatureEnabled;
	@Value("${signature.provider-name}")
	private String signatureProvider;
	@Autowired(required = false)
	CredentialSchemaService credentialSchemaService;


    @Override
    public void run(ApplicationArguments args) throws Exception {
    	logger.info("On Boot starts loading: parent vertex and shard records");
    	entityParenter.ensureKnownParenters();
    	entityParenter.loadDefinitionIndex();
		entityParenter.ensureIndexExists();
		entityParenter.saveIdFormat();
		if(signatureEnabled && Objects.equals(signatureProvider, SignatureV2ServiceImpl.class.getName())) {
			logger.info("On Boot starts loading: credential schemas");
			credentialSchemaService.ensureCredentialSchemas();
		}
		logger.info("Startup completed!");
    }
}
