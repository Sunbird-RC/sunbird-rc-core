package io.opensaber.registry.sink.shard;

import com.fasterxml.jackson.databind.node.IntNode;
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
		DBConnectionInfo connectionInfo = dBConnectionInfoMgr.getConnectionInfo().get(0);
		if (serialNumber instanceof Integer) {
			Integer serNo = (Integer) serialNumber;
			int mod = serNo % 2;
			connectionInfo = dBConnectionInfoMgr.getConnectionInfo().get(mod);

		}
		if (serialNumber instanceof IntNode) {
			Integer serNo = ((IntNode) serialNumber).intValue();
			int mod = serNo % 2;
			connectionInfo = dBConnectionInfoMgr.getConnectionInfo().get(mod);

		}
		return connectionInfo;
	}
}
