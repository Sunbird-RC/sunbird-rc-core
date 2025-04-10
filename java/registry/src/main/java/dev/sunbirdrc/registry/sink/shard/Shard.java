package dev.sunbirdrc.registry.sink.shard;

import dev.sunbirdrc.registry.sink.DatabaseProvider;

/*@Component("shard")
@Scope(value = WebApplicationContext.SCOPE_REQUEST,
        proxyMode = ScopedProxyMode.TARGET_CLASS)*/
public class Shard {

    private String shardId;
    private String shardLabel;
    private DatabaseProvider databaseProvider;

    public DatabaseProvider getDatabaseProvider() {
        return databaseProvider;
    }

    public void setDatabaseProvider(DatabaseProvider databaseProvider) {
        this.databaseProvider = databaseProvider;
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
