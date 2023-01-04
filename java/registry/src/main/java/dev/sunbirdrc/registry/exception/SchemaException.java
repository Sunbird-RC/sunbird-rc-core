package dev.sunbirdrc.registry.exception;

public class SchemaException extends Exception {


    private static final long serialVersionUID = 3337525800481782567L;

    public SchemaException(String message) {
        super(message);
    }

    public SchemaException(String message, Throwable cause) {
        super(message, cause);
    }
}
