package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.registry.exception.UniqueIdentifierException;
import org.neo4j.causalclustering.core.state.machines.id.IdGenerationException;

import javax.json.Json;

public interface IdGenService {

    Object createUniqueID (ObjectNode reqNode) throws UniqueIdentifierException.UnreachableException, UniqueIdentifierException.CreationException;

    Object createUniqueIDsForAnEntity (String enitiyName, JsonNode inputNode) throws UniqueIdentifierException.UnreachableException, UniqueIdentifierException.CreationException;
}
