package io.opensaber.registry.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.registry.middleware.util.Constants;
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
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component("definitionsManager")
public class DefinitionsManager {
    private static Logger logger = LoggerFactory.getLogger(DefinitionsManager.class);

    private Map<String, Definition> definitionMap = new HashMap<>();
    
    private OSResourceLoader osResourceLoader;
    
    @Autowired
    private ResourceLoader resourceLoader;

    /**
     * Loads the definitions from the _schemas folder
     */
    @PostConstruct
    public void loadDefinition() throws Exception {
        
        final ObjectMapper mapper = new ObjectMapper();
        osResourceLoader = new OSResourceLoader(resourceLoader);
        osResourceLoader.loadResource(Constants.RESOURCE_LOCATION);

        for(Entry<String, String> entry : osResourceLoader.getNameContent().entrySet()){
            JsonNode jsonNode = mapper.readTree(entry.getValue());
            Definition definition = new Definition(jsonNode);
            logger.info("loading resource:" + entry.getKey() + " with private field size:"
                    + definition.getOsSchemaConfiguration().getPrivateFields().size() + " & signed fields size:"
                    + definition.getOsSchemaConfiguration().getSignedFields().size());
            definitionMap.putIfAbsent(definition.getTitle(), definition);
        }        
        logger.info("loaded schema resource(s): " + definitionMap.size());
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

}
