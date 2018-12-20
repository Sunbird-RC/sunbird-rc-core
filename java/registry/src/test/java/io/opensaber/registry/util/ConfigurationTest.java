package io.opensaber.registry.util;

import io.opensaber.registry.dao.TPGraphMain;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.sink.DBProviderFactory;
import io.opensaber.registry.sink.DatabaseProviderWrapper;
import io.opensaber.registry.sink.shard.Shard;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * To configure all mock beans 
 * @author Pritha Chattopadhyay
 *
 */
@Profile(Constants.TEST_ENVIRONMENT)
@Configuration
public class ConfigurationTest {
	
	@Bean
	DefinitionsManager definitionsManager(){
		return Mockito.mock(DefinitionsManager.class);
	}
	
	@Bean 
	DBConnectionInfoMgr dbConnectionInfoMgr(){
		return Mockito.mock(DBConnectionInfoMgr.class); 
	}
	
	@Bean 
	EntityCacheManager entityCacheManager(){
		return new EntityCacheManager(definitionsManager(), dbConnectionInfoMgr());

	}
	@Bean
	DefinitionsReader definitionsReader(){
		return Mockito.mock(DefinitionsReader.class); 

	}
	@Bean
	TPGraphMain tpGraphMain(){
		return Mockito.mock(TPGraphMain.class); 

	}
	@Bean
    EntityParenter entityParenter(){
		return Mockito.mock(EntityParenter.class); 
		
	}
	@Bean
	DBProviderFactory dbProviderFactory(){
		return Mockito.mock(DBProviderFactory.class); 

	}
	@Bean
	DatabaseProviderWrapper databaseProviderWrapper(){
		return Mockito.mock(DatabaseProviderWrapper.class); 

	}
	@Bean 
	EntityCache entityCache(){
		return new EntityCache(entityCacheManager());
	}
	@Bean
	Shard shard(){
		return Mockito.mock(Shard.class); 

	}

}
