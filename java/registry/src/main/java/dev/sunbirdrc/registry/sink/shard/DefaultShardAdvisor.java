package dev.sunbirdrc.registry.sink.shard;

import dev.sunbirdrc.registry.model.DBConnectionInfo;
import dev.sunbirdrc.registry.model.DBConnectionInfoMgr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the default shard advisor.
 * The first entry in the application config is chosen as the only default.
 * Must be extended for providing custom shard advisor.
 */
@Component
public class DefaultShardAdvisor implements IShardAdvisor {

	@Autowired
	public DBConnectionInfoMgr dBConnectionInfoMgr;


	/**
	 * Gets the default shard
	 */
	@Override
	public DBConnectionInfo getShard(Object attributeValue) {
		return dBConnectionInfoMgr.getConnectionInfo().get(0);
	}

}
