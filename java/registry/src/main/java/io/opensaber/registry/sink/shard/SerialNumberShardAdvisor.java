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
	 * If serialNum is even, choose first shard  
	 * If serialNum is odd , choose second shard
	 */
	@Override
	public DBConnectionInfo getShard(Object serialNumber) {
		DBConnectionInfo connectionInfo = null;
		if (serialNumber instanceof Integer) {
			Integer serNo = (Integer) serialNumber;
			int mod = serNo % 2;
			connectionInfo = dBConnectionInfoMgr.getConnectionInfo().get(mod);

		}
		return connectionInfo;
	}
}
