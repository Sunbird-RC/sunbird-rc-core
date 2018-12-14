package io.opensaber.registry.sink.shard;

import io.opensaber.registry.model.DBConnectionInfo;

/**
 * This interface must be implemented by all shard advisors.
 */
public interface IShardAdvisor {
	DBConnectionInfo getShard(Object attribute);
}
