package dev.sunbirdrc.registry.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.registry.service.ISearchService;
import dev.sunbirdrc.registry.service.SchemaService;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import dev.sunbirdrc.validators.IValidate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;

import static dev.sunbirdrc.registry.Constants.Schema;
import static dev.sunbirdrc.registry.middleware.util.Constants.*;

@Component
public class SchemaLoader implements ApplicationListener<ContextRefreshedEvent> {
	public static final Logger logger = LoggerFactory.getLogger(SchemaLoader.class);

	@Autowired
	private SchemaService schemaService;

	@Autowired
	private ISearchService searchService;


	@Override
	public void onApplicationEvent(@NotNull ContextRefreshedEvent contextRefreshedEvent) {
		loadSchemasFromDB();
	}

	private void loadSchemasFromDB() {
		ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
		objectNode.set(ENTITY_TYPE, JsonNodeFactory.instance.arrayNode().add(Schema));
		objectNode.set(FILTERS, JsonNodeFactory.instance.objectNode());
		try {
			JsonNode searchResults = searchService.search(objectNode, "");
			for (JsonNode schemaNode : searchResults.get(Schema).get(ENTITY_LIST)) {
				try {
					schemaService.addSchema(JsonNodeFactory.instance.objectNode().set(Schema, schemaNode));
				} catch (Exception e) {
					logger.error("Failed loading schema to definition manager: {}", ExceptionUtils.getStackTrace(e));
				}
			}
			logger.error("Loaded {} schema from DB", searchResults.get(Schema).get(TOTAL_COUNT));
		} catch (IOException e) {
			logger.error("Exception occurred while loading schema from db: {}", ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			logger.error("Exception occurred while searching for schemas: {}", ExceptionUtils.getStackTrace(e));
			logger.error("Make sure, you are running a compatible version of search provider");
		}
	}
}
