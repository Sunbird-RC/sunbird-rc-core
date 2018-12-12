package io.opensaber.registry.util;

public class ShardParentInfo {
    private String shardId;
    private String uuid;

    public ShardParentInfo(String shardId, String uuid) {
        this.shardId = shardId;
        this.uuid = uuid;
    }

    public String getShardId() {
        return shardId;
    }

    public void setShardId(String shardId) {
        this.shardId = shardId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
