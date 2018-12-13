package io.opensaber.registry.shard.advisory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class ShardAdvisor {
    private Map<String, IShardAdvisor> advisors = new HashMap<String, IShardAdvisor>();

    /**
     * Registers the shardAdvisory by property
     *
     * @param propertyName
     * @param shardAdvisory
     */
    public void registerAdvisor(String propertyName, IShardAdvisor shardAdvisory) {
        advisors.put(propertyName, shardAdvisory);
    }

    /**
     * Return ShardAdvice registered with the property
     *
     * @return
     * @throws IOException
     */
    public IShardAdvisor getShardAdvisor(String property) throws IOException {
        IShardAdvisor advisory = advisors.getOrDefault(property, null);
        return advisory;
    }
}
