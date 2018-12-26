package io.opensaber.registry.model;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Pritha Chattopadhyay
 * Auto populates/binds the field values from yaml properties.
 */
@Component("dbConnectionInfoMgr")
@ConfigurationProperties(prefix = "database")
public class DBConnectionInfoMgr {

    /**
     * The value names the unique property to be used by this registry
     * for internal identification purposes.
     */
    private String uuidPropertyName;

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
    /**
     * Instructs which advisor to pick up across each connectionInfo
     * Only one advisor allowed
     */
    private String shardAdvisorClassName;

    public DBConnectionInfoMgr() {

    }

    public List<DBConnectionInfo> getConnectionInfo() {
        return connectionInfo;
    }

    /**
     * To provide a connection info on based of a shard identifier(name)
     *
     * @param shardId
     * @return
     */
    public DBConnectionInfo getDBConnectionInfo(String shardId) {
        for (DBConnectionInfo con : connectionInfo) {
            if (con.getShardId().equalsIgnoreCase(shardId))
                return con;
        }
        return null;
    }

    public String getUuidPropertyName() {
        return uuidPropertyName;
    }

    public void setUuidPropertyName(String uuidPropertyName) {
        this.uuidPropertyName = uuidPropertyName;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setShardProperty(String shardProperty) {
        this.shardProperty = shardProperty;
    }

    public String getShardProperty() {
        return this.shardProperty;
    }

    public void setConnectionInfo(List<DBConnectionInfo> connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

	public String getShardAdvisorClassName() {
		return shardAdvisorClassName;
	}

	public void setShardAdvisorClassName(String shardAdvisorClassName) {
		this.shardAdvisorClassName = shardAdvisorClassName;
	}
}
