package dev.sunbirdrc.registry.middleware;

import java.io.Serial;

public class MiddlewareHaltException extends Exception {

	@Serial
	private static final long serialVersionUID = -4684320522502865642L;

	public MiddlewareHaltException(String message) {
		super(message);
	}

}
