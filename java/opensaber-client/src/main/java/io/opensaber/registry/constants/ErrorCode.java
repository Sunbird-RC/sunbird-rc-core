package io.opensaber.registry.constants;

public enum ErrorCode {

    NODE_MAPPING_NOT_DEFINED("NODE_MAPPING_NOT_DEFINED", 1000);

    private String errorMessage;
    private int errorCode;

    private ErrorCode(String errorMessage, int errorCode) {
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
