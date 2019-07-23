package io.opensaber.views;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Field {

    private String name;
    private String title;
    private boolean display = true;
    private String function;
    private String viewTemplateName;
    private String fetchType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getDisplay() {
        return display;
    }

    public void setDisplay(boolean display) {
        this.display = display;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getViewTemplateName() {
        return viewTemplateName;
    }

    public void setViewTemplateName(String viewTemplateName) {
        this.viewTemplateName = viewTemplateName;
    }

    public String getFetchType() {
        return fetchType;
    }

    public void setFetchType(String fetchType) {
        this.fetchType = fetchType;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        if (null == title) {
            return this.name;
        } else {
            return this.title;
        }
    }
    /**
     * parse function to get the function name 
     * 
     * @return  function name(like: concat)
     */
    public String getFunctioName(){
        String fdName = StringUtils.substring(this.function, this.function.lastIndexOf("/")+1, this.function.indexOf("("));
        if(fdName.isEmpty()){
            throw new IllegalArgumentException("$function reference is not valid! ");
        }
        return fdName;
    }
    /**
     * 
     * @return    array of args name
     */
    public String[] getArgNames(){
        String argNames = this.function.substring(this.function.indexOf("(") + 1, this.function.lastIndexOf(")"));
        return argNames.split(", ");
    }

}
