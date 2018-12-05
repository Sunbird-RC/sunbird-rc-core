package io.opensaber.registry.transform;

public class TransformationException extends Exception {

	private ErrorCode errorCode;

	public TransformationException(String message, Throwable error, ErrorCode errorCode) {
		super(message, error);
		this.errorCode = errorCode;
	}

	public TransformationException(String message, ErrorCode errorCode) {
		super(message);
		this.errorCode = errorCode;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}
}
