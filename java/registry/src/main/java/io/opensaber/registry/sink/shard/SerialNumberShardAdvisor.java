package io.opensaber.registry.sink.shard;

import io.opensaber.registry.model.DBConnectionInfo;
import org.springframework.stereotype.Component;

/**
 * This is an example advisor class.
 * This advisor chooses shards based on the serial number even/odd'ness.
 */
@Component
public class SerialNumberShardAdvisor extends DefaultShardAdvisor {

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
				break;
			}
		}
		return connectionInfo;
	}
}
