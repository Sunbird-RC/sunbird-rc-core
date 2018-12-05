package io.opensaber.registry.middleware;

public class MiddlewareHaltException extends Exception {

	private static final long serialVersionUID = -4684320522502865642L;

	public MiddlewareHaltException(String message) {
		super(message);
	}

}
