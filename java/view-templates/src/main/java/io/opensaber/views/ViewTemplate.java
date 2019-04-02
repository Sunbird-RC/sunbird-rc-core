package io.opensaber.views;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ViewTemplate {
    private static Logger logger = LoggerFactory.getLogger(ViewTemplate.class);

    private String id;
    private String subject;    
    private List<Field> fields;
    private List<FunctionDefinition> functionDefinitions;
    
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getSubject() {
        return subject;
    }
    public void setSubject(String subject) {
        this.subject = subject;
    }
    public List<FunctionDefinition> getFunctionDefinitions() {
        return functionDefinitions;
    }
    public void setFunctionDefinitions(List<FunctionDefinition> functionDefinitions) {
        this.functionDefinitions = functionDefinitions;
    }
    public List<Field> getFields() {
        return fields;
    }
    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    /**
     * return a FunctionDefinition for a given function name 

     * @param name    function name (like concat)
     * @return
     */
    public FunctionDefinition getFunctionDefinition(String name) {
        FunctionDefinition functionDef = null;
        for (FunctionDefinition fd : this.getFunctionDefinitions()) {
            if (fd.getName().compareTo(name) == 0) {
                functionDef = fd;
            }
        }
        if (functionDef == null) {
            logger.error("No function definition specified for function - " + name);
            throw new IllegalArgumentException("No function definition specified for function - " + name);
        }
        return functionDef;
    }
}
