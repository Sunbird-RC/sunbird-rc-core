package dev.sunbirdrc.registry.dao;

public class NotFoundException extends Exception {

    public NotFoundException(String entityType, String uuidPropertyValue) {
        super(String.format("Record not found of type %s with id %s", entityType, uuidPropertyValue));
    }

}
