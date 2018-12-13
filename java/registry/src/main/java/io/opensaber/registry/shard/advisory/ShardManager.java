package io.opensaber.registry.shard.advisory;

import java.io.IOException;

import io.opensaber.registry.exception.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.opensaber.registry.model.DBConnectionInfo;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.registry.service.SearchService;
import io.opensaber.registry.sink.DBProviderFactory;
import io.opensaber.registry.sink.DatabaseProvider;

@Component("shardManager")
public class ShardManager {
	
	@Autowired
	private DBConnectionInfoMgr dbConnectionInfoMgr;
	@Autowired
	private DBProviderFactory dbProviderFactory;
	@Autowired
	private IShardAdvisor shardAdvisor;	
	@Autowired
	private RegistryService registryService;
	@Autowired
	private SearchService searchService;
	
	/**
	 * intiatiate a DBShard and ensure activating a databaseProvider.
	 * used for add end point. 
	 * @param attributeValue
	 * @throws IOException
	 */
	private DatabaseProvider activateDbShard(Object attributeValue) throws CustomException {
		DBConnectionInfo connectionInfo = shardAdvisor.getShard(attributeValue);
	    DatabaseProvider databaseProvider = dbProviderFactory.getInstance(connectionInfo);
		registryService.setDatabaseProvider(databaseProvider);
		searchService.setDatabaseProvider(databaseProvider);
		return databaseProvider;
	}

	public String getShardProperty() {
		return dbConnectionInfoMgr.getShardProperty();
	}

	public DatabaseProvider getDatabaseProvider(Object attributeValue) throws CustomException {
		return activateDbShard(attributeValue);
	}

	public DatabaseProvider getDefaultDatabaseProvider() throws CustomException {
		return activateDbShard(null);
	}

}
