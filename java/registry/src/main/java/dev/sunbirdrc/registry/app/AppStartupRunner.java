package dev.sunbirdrc.registry.app;

import dev.sunbirdrc.registry.service.CredentialSchemaService;
import dev.sunbirdrc.registry.util.EntityParenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AppStartupRunner implements ApplicationRunner {

    private static Logger logger = LoggerFactory.getLogger(AppStartupRunner.class);

	@Autowired
	EntityParenter entityParenter;

	@Autowired
	CredentialSchemaService credentialSchemaService;


    @Override
    public void run(ApplicationArguments args) throws Exception {
    	logger.info("On Boot starts loading: parent vertex and shard records");
    	entityParenter.ensureKnownParenters();
    	entityParenter.loadDefinitionIndex();
		entityParenter.ensureIndexExists();
		entityParenter.saveIdFormat();
		logger.info("On Boot starts loading: credential schemas");
		credentialSchemaService.ensureCredentialSchemas();
		logger.info("Startup completed!");
    }
}
