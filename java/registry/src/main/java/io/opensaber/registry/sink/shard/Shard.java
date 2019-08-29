package io.opensaber.registry.sink.shard;

import io.opensaber.registry.sink.DatabaseProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
/*@Component("shard")
@Scope(value = WebApplicationContext.SCOPE_REQUEST,
        proxyMode = ScopedProxyMode.TARGET_CLASS)*/
public class Shard {

	private String shardId;
	private String shardLabel;
	private DatabaseProvider databaseProvider;

	public void setDatabaseProvider(DatabaseProvider databaseProvider) {
		this.databaseProvider = databaseProvider;
	}

	public DatabaseProvider getDatabaseProvider() {
		return databaseProvider;
	}

	public String getShardId() {
		return shardId;
	}

	public void setShardId(String shardId) {
		this.shardId = shardId;
	}

	public String getShardLabel() {
		return shardLabel;
	}

	public void setShardLabel(String shardLabel) {
		this.shardLabel = shardLabel;
	}

}
