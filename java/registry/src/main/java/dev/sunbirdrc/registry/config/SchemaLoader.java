package dev.sunbirdrc.registry.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.pojos.APIMessage;
import dev.sunbirdrc.registry.service.ISearchService;
import dev.sunbirdrc.registry.util.DefinitionsManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static dev.sunbirdrc.registry.Constants.Schema;

@Component
public class SchemaLoader implements ApplicationListener<ContextRefreshedEvent> {
    public static final Logger logger = LoggerFactory.getLogger(SchemaLoader.class);

    @Autowired
    private ISearchService searchService;

    @Autowired
    private DefinitionsManager definitionsManager;

    @Override
    public void onApplicationEvent(@NotNull ContextRefreshedEvent contextRefreshedEvent) {
        loadSchemasFromDB();
    }

    private void loadSchemasFromDB() {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.set("entityType", JsonNodeFactory.instance.arrayNode().add(Schema));
        objectNode.set("filters", JsonNodeFactory.instance.objectNode());
        try {
            JsonNode searchResults = searchService.search(objectNode);
            searchResults.get(Schema).forEach(schemaNode -> {
                definitionsManager.appendNewDefinition(schemaNode.get(Schema.toLowerCase()));
            });
            logger.info("Loaded {} schema from DB", searchResults.get(Schema).size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
