package dev.sunbirdrc.registry.exception;

public class UnreachableException extends CustomException {
    private static final long serialVersionUID = 5384120386096139083L;

    public UnreachableException(String message) {
        super("Unable to reach service: " + message);
    }
}
