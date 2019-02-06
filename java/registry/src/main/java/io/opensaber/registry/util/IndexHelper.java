package io.opensaber.registry.util;

import io.opensaber.registry.middleware.util.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IndexHelper {
    private static Logger logger = LoggerFactory.getLogger(IndexHelper.class);
    
    /**
     * Holds mapping for each shard & each definitions and its index status 
     * key = shardId+definitionName 
     * value = true/false
     */
    private Map<String, Boolean> definitionIndexMap = new ConcurrentHashMap<String, Boolean>();
 
    public void setDefinitionIndexMap(Map<String, Boolean> definitionIndexMap) {
        this.definitionIndexMap.putAll(definitionIndexMap);
    }
   
    public void updateDefinitionIndex(String label, String definitionName, boolean flag){
        String key = label + definitionName;
        definitionIndexMap.put(key, flag);
    }

    /**
     * Checks any new index available for index creation
     * 
     * @param parentVertex
     * @param definition
     * @return
     */
    public boolean isIndexPresent(Definition definition, String shardId) {
        String defTitle = definition.getTitle();
        boolean isIndexPresent = definitionIndexMap.getOrDefault(shardId + defTitle, false);
        logger.debug("isIndexPresent flag for {}: {}", defTitle, isIndexPresent);
        return isIndexPresent;       
    }

    /**
     * Identifies new fields for creating index. Parent vertex are always have
     * INDEX_FIELDS and UNIQUE_INDEX_FIELDS property
     * 
     * @param parentVertex
     * @param fields
     * @param isUnique
     */
    public List<String> getNewFields(Vertex parentVertex, List<String> fields, boolean isUnique) {
        List<String> newFields = new ArrayList<>();
        String propertyName = isUnique ? Constants.UNIQUE_INDEX_FIELDS : Constants.INDEX_FIELDS;
        String values = "";
        try {
            values = (String) parentVertex.property(propertyName).value();
        } catch (java.lang.IllegalStateException ise) {
            // The property doesn't exist.
            values = "";
        }
        for (String field : fields) {
            if (!values.contains(field) && !newFields.contains(field))
                newFields.add(field);
        }
        return newFields;
    }

}
