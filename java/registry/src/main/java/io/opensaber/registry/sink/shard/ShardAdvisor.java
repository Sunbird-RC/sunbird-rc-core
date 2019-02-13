package io.opensaber.registry.sink.shard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class ShardAdvisor {
	private static Logger logger = LoggerFactory.getLogger(ShardAdvisor.class);

	/**
	 * Return ShardAdvice invoked by given a ShardAdvisorClassName
	 *
	 * @return
	 * @throws IOException
	 */
	public IShardAdvisor getInstance(String advisorClassName) {

		IShardAdvisor advisor = new DefaultShardAdvisor();
		try {
			if (advisorClassName != null) {
				advisor = instantiateAdvisor(advisorClassName);
				logger.info("Invoked shard advisor class with classname: " + advisorClassName);
			}
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
				| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			logger.error("Shard advisor class {} cannot be instantiate with exception:", advisorClassName, e);
		}

		return advisor;
	}

	private IShardAdvisor instantiateAdvisor(String advisorClassName)
			throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class<?> advisorClass = Class.forName(advisorClassName);
		IShardAdvisor advisor = (IShardAdvisor) advisorClass.newInstance();
		return advisor;

	}

}
