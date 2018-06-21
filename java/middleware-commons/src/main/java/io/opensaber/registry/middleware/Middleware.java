package io.opensaber.registry.middleware;

import java.io.IOException;
import java.util.Map;

public interface Middleware {

	/**
	 * This method executes the middleware logic
	 * @param mapData
	 * @return
	 * @throws IOException
	 * @throws MiddlewareHaltException
	 */
	Map<String,Object> execute(Map<String,Object> mapData) throws IOException, MiddlewareHaltException;

	/**
	 * This method chains the flow to the next middleware that needs to be executed
	 * @param mapData
	 * @return
	 * @throws IOException
	 */
	Map<String,Object> next(Map<String,Object> mapData) throws IOException;

}
