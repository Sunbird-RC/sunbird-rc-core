package dev.sunbirdrc.registry.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.registry.service.ISearchService;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import dev.sunbirdrc.validators.IValidate;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static dev.sunbirdrc.registry.Constants.Schema;
import static dev.sunbirdrc.registry.middleware.util.Constants.ENTITY_TYPE;
import static dev.sunbirdrc.registry.middleware.util.Constants.FILTERS;

@Component
public class SchemaLoader implements ApplicationListener<ContextRefreshedEvent> {
	public static final Logger logger = LoggerFactory.getLogger(SchemaLoader.class);

	@Autowired
	private ISearchService searchService;

	@Autowired
	private IDefinitionsManager definitionsManager;

	@Autowired
	private IValidate validator;

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public void onApplicationEvent(@NotNull ContextRefreshedEvent contextRefreshedEvent) {
		loadSchemasFromDB();
	}

	private void loadSchemasFromDB() {
		ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
		objectNode.set(ENTITY_TYPE, JsonNodeFactory.instance.arrayNode().add(Schema));
		objectNode.set(FILTERS, JsonNodeFactory.instance.objectNode());
		try {
			JsonNode searchResults = searchService.search(objectNode);
			for (JsonNode schemaNode : searchResults.get(Schema)) {
				JsonNode schema = schemaNode.get(Schema.toLowerCase());
				definitionsManager.appendNewDefinition(schema);
				validator.addDefinitions(schema);
			}
			logger.info("Loaded {} schema from DB", searchResults.get(Schema).size());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
