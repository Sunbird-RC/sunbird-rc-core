package io.opensaber.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.util.ReadConfigurator;

public interface RegistryService {

	 HealthCheckResponse health() throws Exception;

	 boolean deleteEntityById(String id) throws AuditFailedException, RecordNotFoundException;

	 String addEntity(String jsonString) throws Exception;

     JsonNode getEntity(String id, ReadConfigurator configurator);

	 void updateEntity(String jsonString) throws Exception;
}
