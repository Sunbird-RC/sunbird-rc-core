package dev.sunbirdrc.registry.middleware;

import dev.sunbirdrc.pojos.APIMessage;

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
