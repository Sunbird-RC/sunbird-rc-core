package io.opensaber.registry.sink.shard;

import io.opensaber.registry.model.DBConnectionInfo;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import org.springframework.stereotype.Component;

/**
 * This is the default shard advisor.
 * The first entry in the application config is chosen as the only default.
 */
@Component
public class DefaultShardAdvisor implements IShardAdvisor {

	private DBConnectionInfoMgr dBConnectionInfoMgr;

	public DefaultShardAdvisor(DBConnectionInfoMgr dBConnectionInfoMgr) {
		this.dBConnectionInfoMgr = dBConnectionInfoMgr;
	}

	/**
	 * Gets the default shard
	 */
	@Override
	public DBConnectionInfo getShard(Object attributeValue) {
		return dBConnectionInfoMgr.getConnectionInfo().get(0);
	}
}
