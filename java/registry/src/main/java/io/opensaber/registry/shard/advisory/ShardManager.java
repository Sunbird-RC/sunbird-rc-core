package io.opensaber.registry.shard.advisory;

import java.io.IOException;

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
	private DBConnectionInfoMgr dBConnectionInfoMgr;
	@Autowired
	private DBProviderFactory dbProviderFactory;
	@Autowired
	private IShardAdvisor shardAdvisor;	
	@Autowired
	private RegistryService registryService;
	@Autowired
	private SearchService searchService;	
	private DatabaseProvider databaseProvider;
	
	/**
	 * intiatiate a DBShard and ensure activating a databaseProvider.
	 * used for add end point. 
	 * @param attributeValue
	 * @throws IOException
	 */
	public void activateDbShard(Object attributeValue) throws IOException{
		DBConnectionInfo connectionInfo = shardAdvisor.getShard(attributeValue);
	    databaseProvider = dbProviderFactory.getInstance(connectionInfo);
		registryService.setDatabaseProvider(databaseProvider);
		searchService.setDatabaseProvider(databaseProvider);
	}

	public String getShardProperty() {
		return dBConnectionInfoMgr.getShardProperty();
	}

	public DatabaseProvider getDatabaseProvider(){
		return databaseProvider;
	}
	

}
