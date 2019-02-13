package io.opensaber.registry.middleware;

import io.opensaber.pojos.APIMessage;

import java.io.IOException;

public interface Middleware {

	/**
	 * This method executes the middleware logic
	 * 
	 * @param apiMessage
	 * @return
	 * @throws IOException
	 * @throws MiddlewareHaltException
	 */
	boolean execute(APIMessage apiMessage) throws IOException, MiddlewareHaltException;

}
