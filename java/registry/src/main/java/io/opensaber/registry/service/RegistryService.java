package io.opensaber.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jsonldjava.core.JsonLdError;
import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.registry.exception.*;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.util.ReadConfigurator;
import org.apache.jena.rdf.model.Model;

import java.io.IOException;
import java.util.List;

public interface RegistryService {

	 HealthCheckResponse health() throws Exception;

	 boolean deleteEntityById(String id) throws AuditFailedException, RecordNotFoundException;

	 String addEntity(String shardId, String jsonString) throws Exception;

     JsonNode getEntity(String id, ReadConfigurator configurator);

	 void updateEntity(String jsonString) throws Exception;
}
