package io.opensaber.registry.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
/**
 * 
 * @author Pritha Chattopadhyay
 * Auto populates/binds the field values from yaml properties.
 *
 */
@Component
@ConfigurationProperties(prefix = "database")
public class DBConnectionInfoMgr {
	
	/**
	 * only one type of database provider as the target
	 * as of today.
	 */
	private String provider;
	/**
	 * Only one property is allowed.
	 */
	private String shardProperty;
	/**
	 * Each DBConnectionInfo is a shard connection information.
	 */
	private List<DBConnectionInfo> connectionInfo = new ArrayList<>();
	
	public DBConnectionInfoMgr(){
		
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public List<DBConnectionInfo> getConnectionInfo() {
		return connectionInfo;
	}

	public void setConnectionInfo(List<DBConnectionInfo> connectionInfo) {
		this.connectionInfo = connectionInfo;
	}

	/**
	 * To provide a connection info on based of a shard identifier(name)
	 * @param shardId
	 * @return
	 */
	public DBConnectionInfo getDBConnectionInfo(String shardId){
		for(DBConnectionInfo con: connectionInfo){
			if(con.getShardId().equalsIgnoreCase(shardId))
				return con;
		}
		return null;
	}

	public String getShardProperty() {
		return shardProperty;
	}

	public void setShardProperty(String shardProperty) {
		this.shardProperty = shardProperty;
	}


}
