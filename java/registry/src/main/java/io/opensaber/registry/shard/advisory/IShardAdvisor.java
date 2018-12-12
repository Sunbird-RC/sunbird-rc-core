package io.opensaber.registry.shard.advisory;

import io.opensaber.registry.model.DBConnectionInfo;

public interface IShardAdvisor {
	
	public DBConnectionInfo getShard(Object attribute);
	public String shardId();
}
