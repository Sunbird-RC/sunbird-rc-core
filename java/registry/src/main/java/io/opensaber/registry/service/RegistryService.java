package io.opensaber.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.registry.util.ReadConfigurator;

public interface RegistryService {

	HealthCheckResponse health() throws Exception;

	void deleteEntityById(String id) throws Exception;

	String addEntity(String jsonString) throws Exception;

	JsonNode getEntity(String id, String rootLabel, ReadConfigurator configurator) throws Exception;

	void updateEntity(String id, String jsonString) throws Exception;

}
