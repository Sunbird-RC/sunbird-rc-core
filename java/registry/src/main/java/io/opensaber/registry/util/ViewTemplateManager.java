package io.opensaber.registry.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.opensaber.views.Field;
import io.opensaber.views.ViewTemplate;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class ViewTemplateManager {
	
	private static Logger logger = LoggerFactory.getLogger(ViewTemplateManager.class);

	
    public static final String viewLocation = "classpath*:views/*.json";
    private static final String viewTemplateId = "viewTemplateId";
    private static final String viewTemplate = "viewTemplate";

    private OSResourceLoader osResourceLoader;
    private ObjectMapper mapper = new ObjectMapper();
    private Map<String, ViewTemplate> templates = new HashMap<>();
    
    @Autowired
    private ResourceLoader resourceLoader;

	@Autowired
    private DefinitionsManager definitionsManager;

    /**
     * Loads the templates from the views folder
     */
    @PostConstruct
	public void loadTemplates() throws Exception {
    	osResourceLoader = new OSResourceLoader(resourceLoader);
		osResourceLoader.loadResource(viewLocation);
		for (Entry<String, String> jsonNode : osResourceLoader.getNameContent().entrySet()) {
			try {
				ViewTemplate template = mapper.readValue(jsonNode.getValue(), ViewTemplate.class);
				templates.put(jsonNode.getKey(), template);
			} catch (Exception e) {
				logger.error("ViewTemplate could not be create for {}", jsonNode.getKey());
			}
		}

	}
    
    /**
     * Returns the view template based on the request parameter viewTemplateId, viewTemplate 
     * 
     * @param requestNode
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
	public ViewTemplate getViewTemplate(JsonNode requestNode)
			throws JsonParseException, JsonMappingException, IOException {

		ViewTemplate viewTemp = null;
		String name = null;
		try {
			if (requestNode.has(viewTemplateId)) {
				name = requestNode.get(viewTemplateId).asText();
				logger.info("Applying view template {}", name);
				viewTemp = templates.get(name);
				if(viewTemp == null)
					logger.error("view template for {} not found!", name);
			} else if (requestNode.has(viewTemplate)) {
				logger.info("Applying passed in view template...");
				viewTemp = getViewTemplateByContent(requestNode.get(viewTemplate).toString());
			}
		} catch (Exception e) {
			logger.error("Bad request to create a view template, {}", e);
		}
		return viewTemp;
	}
    
	private ViewTemplate getViewTemplateByContent(String templateContent)
			throws JsonParseException, JsonMappingException, IOException {
		return mapper.readValue(templateContent, ViewTemplate.class);
	}

	public boolean isPrivateFieldEnabled(ViewTemplate viewTemplate, String entityType){
		boolean privateFieldEnabled = false;
		List<Field> fieldList = viewTemplate.getFields();
		Definition definition = definitionsManager.getDefinition(entityType);
		List<String> privateFields = definition.getOsSchemaConfiguration().getPrivateFields();
		for(Field field : fieldList) {
			if(privateFields.contains(field.getName())){
				privateFieldEnabled = true;
				break;
			}
		}
		return privateFieldEnabled;
	}
    
}
