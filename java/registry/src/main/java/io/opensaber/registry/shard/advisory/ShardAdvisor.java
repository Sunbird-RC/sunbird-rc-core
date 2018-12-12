package io.opensaber.registry.shard.advisory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class ShardAdvisor {
	
	private Map<String,IShardAdvisor> advisors = new HashMap<String,IShardAdvisor>();
	/**
	 * Registers the shardAdvisory by property 
	 * @param property
	 * @param shardAdvisory
	 */
	public void registerAdvisor(String property, IShardAdvisor shardAdvisory){		
		advisors.put(property, shardAdvisory);		
		
	}
	/**
	 * Return ShardAdvice registered with the property
	 * @return
	 * @throws IOException 
	 */
	public IShardAdvisor getShardAdvisor(String property) throws IOException{
		IShardAdvisor advisory = null;
		if(advisors.keySet().contains(property))
			advisory = advisors.get(property);
		else
			throw new IOException("Not found advisory for given property. Cosider registering this property.");
		return advisory;
	}
	
	

}
