package io.opensaber.registry.app;

import io.opensaber.registry.util.EntityParenter;
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


    @Override
    public void run(ApplicationArguments args) throws Exception {
    	logger.info("On Boot starts loading: parent vertex and shard records");
    	entityParenter.ensureKnownParenters();
    	entityParenter.loadDefinitionIndex();
    	entityParenter.ensureIndexExists();
    }
}
