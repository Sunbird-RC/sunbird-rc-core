package io.opensaber.registry.exception;

public class SignatureException extends Exception {

	private static final long serialVersionUID = -6315798195661762882L;

	public class CreationException extends CustomException {
		private static final long serialVersionUID = 6174717850058203376L;

		public CreationException(String msg) {
			super("Unable to create signature: " + msg);
		}
	}

	public class VerificationException extends CustomException {

		private static final long serialVersionUID = 4996784337180620650L;

		public VerificationException(String message) {
			super("Unable to verify signature: " + message);
		}
	}

	public class UnreachableException extends CustomException {

		private static final long serialVersionUID = 5384120386096139083L;

		public UnreachableException(String message) {
			super("Unable to reach service: " + message);
		}
	}

	public class KeyNotFoundException extends CustomException {

		private static final long serialVersionUID = 8311355815972497247L;

		public KeyNotFoundException(String message) {
			super("Unable to get key: " + message);
		}
	}
}
