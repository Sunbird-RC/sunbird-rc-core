package io.opensaber.registry.sink.shard;

import io.opensaber.registry.exception.CustomException;
import io.opensaber.registry.model.DBConnectionInfo;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.sink.DBProviderFactory;
import io.opensaber.registry.sink.DatabaseProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component("shardManager")
public class ShardManager {

	private static Logger logger = LoggerFactory.getLogger(ShardManager.class);
	@Autowired
	private DBConnectionInfoMgr dbConnectionInfoMgr;
	@Autowired
	private DBProviderFactory dbProviderFactory;
	@Autowired
	private IShardAdvisor shardAdvisor;

	private Shard shard;


	/**
	 * intiatiate a DBShard and ensure activating a databaseProvider. used for
	 * add end point.
	 *
	 * @param attributeValue
	 * @throws IOException
	 */
	private Shard activateDbShard(Object attributeValue) {
		DBConnectionInfo connectionInfo = shardAdvisor.getShard(attributeValue);
		DatabaseProvider databaseProvider = dbProviderFactory.getInstance(connectionInfo);
		shard = new Shard();
		shard.setShardId(connectionInfo.getShardId());
		shard.setShardLabel(connectionInfo.getShardLabel());
		shard.setDatabaseProvider(databaseProvider);
		logger.info("Activated shard "+connectionInfo.getShardId()+" for attribute value "+attributeValue);
		return shard;
	}

	public String getShardProperty() {
		return dbConnectionInfoMgr.getShardProperty();
	}
	/**
	 * activates a shard (Default or others) and returns it.
	 * @param attributeValue
	 * @return
	 * @throws CustomException
	 */
	public Shard getShard(Object attributeValue) {

		if(attributeValue != null){
			activateDbShard(attributeValue);
		}else{
			activateDbShard(null);
		}
		return shard;
	}
	/**
	 * Default shard return first shard.
	 * Atleast one shard configuration is mandatory.
	 * @return
	 * @throws CustomException
	 */
	public Shard getDefaultShard() {
		activateDbShard(null);
		return shard;
	}

	/**
	 * activate a shard given a shardId from entity cache
	 * use this for read operation
	 * @param shardId
	 * @return
	 * @throws CustomException
	 */
	public Shard activateShard(String shardId) {
		if (shardId != null) {
			DBConnectionInfo connectionInfo = dbConnectionInfoMgr.getDBConnectionInfo(shardId);
			DatabaseProvider databaseProvider = dbProviderFactory.getInstance(connectionInfo);
			shard = new Shard();
			shard.setShardId(shardId);
			shard.setShardLabel(connectionInfo.getShardLabel());
			shard.setDatabaseProvider(databaseProvider);
		} else {
			logger.info("Default shard is activated");
			shard = activateDbShard(null);

		}
		return shard;
	}

	public Shard getShardInstance(String shardId) {
		Shard thisShard = new Shard();
		if (shardId != null) {
			DBConnectionInfo connectionInfo = dbConnectionInfoMgr.getDBConnectionInfo(shardId);
			DatabaseProvider databaseProvider = dbProviderFactory.getInstance(connectionInfo);
			thisShard.setShardId(connectionInfo.getShardId());
			thisShard.setDatabaseProvider(databaseProvider);
		}

		return thisShard;
	}

}
