package io.opensaber.registry.kernel.util;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogGraphEvent {

	private static final Logger graphEventLogger = LoggerFactory.getLogger("GraphEventLogger");
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
