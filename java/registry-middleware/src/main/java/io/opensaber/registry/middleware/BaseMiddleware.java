package io.opensaber.registry.middleware;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 * @author jyotsna
 *
 */
public interface BaseMiddleware {
	
	/**
	 * This method executes the middleware logic
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	public void execute(HttpServletRequest request, HttpServletResponse response) throws IOException;
	
	/**
	 * This method chains the flow to the next middleware that needs to be executed
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	public void next(HttpServletRequest request, HttpServletResponse response) throws IOException;

}
