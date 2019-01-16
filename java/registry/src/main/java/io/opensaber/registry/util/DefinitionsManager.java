package io.opensaber.registry.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component("definitionsManager")
public class DefinitionsManager {
    private static Logger logger = LoggerFactory.getLogger(DefinitionsManager.class);
    private final static String TITLE = "title";

    @Autowired
    private DefinitionsReader definitionsReader;
    private Map<String, Definition> definitionMap = new HashMap<>();

    /**
     * Loads the definitions from the _schemas folder
     */
    @PostConstruct
    public void loadDefinition() throws Exception {

        final ObjectMapper mapper = new ObjectMapper();
        Resource[] resources = definitionsReader.getResources("classpath:public/_schemas/*.json");
        for (Resource resource : resources) {
            String jsonContent = getContent(resource);
            JsonNode jsonNode = mapper.readTree(jsonContent);
            Definition definition = new Definition(jsonNode);
            logger.info("loading resource:" + resource.getFilename() + " with private field size:"
                    + definition.getOsSchemaConfiguration().getPrivateFields().size() + " & signed fields size:"
                    + definition.getOsSchemaConfiguration().getSignedFields().size());
            definitionMap.putIfAbsent(definition.getTitle(), definition);
        }
        logger.info("loaded resource(s): " + definitionMap.size());

    }

    /**
     * Returns the title for all definitions loaded
     * 
     * @return
     */
    public Set<String> getAllKnownDefinitions() {
        return definitionMap.keySet();
    }

    /**
     * Returns all definitions that are loaded
     * 
     * @return
     */
    public List<Definition> getAllDefinitions() {
        List<Definition> definitions = new ArrayList<>();
        for (Entry<String, Definition> entry : definitionMap.entrySet()) {
            definitions.add(entry.getValue());
        }
        return definitions;
    }

    /**
     * Provide a definition by given title which is already loaded
     * 
     * @param title
     * @return
     */
    public Definition getDefinition(String title) {
        return definitionMap.getOrDefault(title, null);
    }

    /**
     * Returns a content of resource
     * 
     * @param resource
     * @return
     */
    private String getContent(Resource resource) {
        String content = null;
        try {
            File file = resource.getFile();
            content = new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            logger.error("Cannot load resource " + resource.getFilename());

        }
        return content;
    }
}
