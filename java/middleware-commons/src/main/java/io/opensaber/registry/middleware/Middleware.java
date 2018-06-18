package io.opensaber.registry.middleware;

import java.io.IOException;
import java.util.Map;

/**
 * 
 * @author jyotsna
 *
 */
public interface Middleware {
	
	/**
	 * This method executes the middleware logic
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws MiddlewareHaltException 
	 */
	public Map<String,Object> execute(Map<String,Object> mapData) throws IOException, MiddlewareHaltException;
	
	/**
	 * This method chains the flow to the next middleware that needs to be executed
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	public Map<String,Object> next(Map<String,Object> mapData) throws IOException;

}
