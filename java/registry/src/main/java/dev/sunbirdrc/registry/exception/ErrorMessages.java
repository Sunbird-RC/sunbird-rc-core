package dev.sunbirdrc.registry.exception;

public class ErrorMessages {
    public final static String UNAUTHORIZED_EXCEPTION_MESSAGE = "User is not allowed to access the entity";
    public final static String UNAUTHORIZED_OPERATION_MESSAGE = "User is not allowed to perform the operation on this entity";
    public final static String INVALID_OPERATION_EXCEPTION_MESSAGE = "User is trying to update someone's data";

    public final static String NOT_PART_OF_THE_SYSTEM_EXCEPTION = "Schema '%s' not found";

    public final static String INVALID_ID_MESSAGE = "Invalid ID";
    public final static String NOT_ALLOWED_FOR_PUBLISHED_SCHEMA = "Schema delete not allowed for a published schema";
}
