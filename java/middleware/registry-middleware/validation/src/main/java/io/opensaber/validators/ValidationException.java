package io.opensaber.validators;

public class ValidationException extends Exception {
	public ValidationException(String msg) {
		super("Invalid data " + msg);
	}

	public static class NoImplementationFoundException extends Exception {
		public NoImplementationFoundException(String msg) {
			super("Can't find an validator implementation for type " + msg);
		}
	}
}