package io.opensaber.registry.exception;

import io.opensaber.registry.constants.ErrorCode;

public class RegistryServiceUnavailableException extends Exception {

    private ErrorCode errorCode;

    public RegistryServiceUnavailableException(String message, Throwable error, ErrorCode errorCode) {
        super(message, error);
        this.errorCode = errorCode;
    }

    public RegistryServiceUnavailableException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
