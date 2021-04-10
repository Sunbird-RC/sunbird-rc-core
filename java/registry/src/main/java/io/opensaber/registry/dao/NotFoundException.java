package io.opensaber.registry.dao;

public class NotFoundException extends Exception {
    private final String entityType;
    private final String osid;

    public NotFoundException(String entityType, String osid) {
        super(String.format("Record not found of type %s with id %s", entityType, osid));
        this.entityType = entityType;
        this.osid = osid;
    }

}
