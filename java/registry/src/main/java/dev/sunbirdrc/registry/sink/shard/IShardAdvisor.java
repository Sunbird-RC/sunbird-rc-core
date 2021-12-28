package dev.sunbirdrc.registry.sink.shard;

import dev.sunbirdrc.registry.model.DBConnectionInfo;

/**
 * This interface must be implemented by all shard advisors.
 */
public interface IShardAdvisor {
	DBConnectionInfo getShard(Object attribute);
}
