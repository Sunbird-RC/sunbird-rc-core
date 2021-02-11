package io.opensaber.registry.exception;

public class AuditFailedException extends Exception {

	private static final long serialVersionUID = 8531501706088259947L;
	
	public AuditFailedException(String message) {
		super(message);
	}
}