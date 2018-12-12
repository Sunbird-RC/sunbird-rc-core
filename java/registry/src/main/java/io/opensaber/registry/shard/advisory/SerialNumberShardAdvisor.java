package io.opensaber.registry.shard.advisory;

import org.springframework.stereotype.Component;

import io.opensaber.registry.model.DBConnectionInfo;
import io.opensaber.registry.model.DBConnectionInfoMgr;

@Component
public class SerialNumberShardAdvisor implements IShardAdvisor {

	private String shardId;
	private DBConnectionInfoMgr dBConnectionInfoMgr;

	public SerialNumberShardAdvisor(DBConnectionInfoMgr dBConnectionInfoMgr) {
		this.dBConnectionInfoMgr = dBConnectionInfoMgr;
	}

	/**
	 * Based on serialNum, choosing the shard.
	 * If serialNum is even, choose shard with id ‘shard1’ 
	 * If serialNum is odd , choose shard with id 'shard2'
	 */
	@Override
	public DBConnectionInfo getShard(Object serialNumber) {

		DBConnectionInfo connectionInfo = null;
		if (serialNumber instanceof Integer) {
			Integer serNo = (Integer) serialNumber;
			switch (serNo % 2) {
			case 0:
				connectionInfo = dBConnectionInfoMgr.getDBConnectionInfo("shard1");
				break;
			case 1:
				connectionInfo = dBConnectionInfoMgr.getDBConnectionInfo("shard2");
				break;
			default:
				connectionInfo = dBConnectionInfoMgr.getDBConnectionInfo("shard1");
				break;
			}
		}
		shardId = connectionInfo.getShardId();
		return connectionInfo;
	}

	@Override
	public String shardId() {
		return shardId;
	}
}
