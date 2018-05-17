package io.opensaber.registry.kernel.util;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

public class LogGraphEvent {

	private static final Logger graphEventLogger = LogManager.getLogger("GraphEventLogger");
	private static ObjectMapper mapper = new ObjectMapper();
	
	
	public static void pushMessageToLogger(List<Map<String, Object>> messages) {
		if (null == messages || messages.size() <= 0) return; 
		for (Map<String, Object> message : messages) {
			try{
				String jsonMessage = mapper.writeValueAsString(message);
				if (StringUtils.isNotBlank(jsonMessage))
					graphEventLogger.info(jsonMessage);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
}
