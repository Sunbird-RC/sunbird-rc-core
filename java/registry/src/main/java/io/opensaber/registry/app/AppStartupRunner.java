package io.opensaber.registry.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import io.opensaber.registry.util.EntityParenter;

@Component
public class AppStartupRunner implements ApplicationRunner {
	
	@Autowired
	EntityParenter entityParenter;
 
    @Override
    public void run(ApplicationArguments args) throws Exception {
    	entityParenter.ensureKnownParenters();
    }
}
