package dev.sunbirdrc.registry.service;

import dev.sunbirdrc.pojos.HealthIndicator;
import dev.sunbirdrc.pojos.UniqueIdentifierField;
import dev.sunbirdrc.registry.exception.CustomException;

import java.util.List;
import java.util.Map;

public interface IIdGenService extends HealthIndicator {

    Map<String, String> generateId(List<UniqueIdentifierField> uniqueIdentifierFields) throws CustomException;

    void saveIdFormat(List<UniqueIdentifierField> uniqueIdentifierFields) throws CustomException;
}