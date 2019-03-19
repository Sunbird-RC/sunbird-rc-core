package io.opensaber.registry.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
/**
 * Holds _osconfig properties for a schema  
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OSSchemaConfiguration {
    /**
     * holds field name(s) to be encrypted
     */
    private List<String> privateFields =  new ArrayList<>();
    /**
     * Holds field name(s) to be used for signature
     */
    private List<String> signedFields =  new ArrayList<>();
    /**
     * Holds field name(s) to be used for index
     */
    private List<String> indexFields =  new ArrayList<>();
    /**
     * Holds field name(s) to be used for unique index
     */
    private List<String> uniqueIndexFields =  new ArrayList<>();
    /**
     * Holds fields name(s) to be used for auditing
     */
    private List<String> systemFields =  new ArrayList<>();

    public List<String> getPrivateFields() {
        return privateFields;
    }

    public void setPrivateFields(List<String> privateFields) {
        this.privateFields = privateFields;
    }

    public List<String> getSignedFields() {
        return signedFields;
    }

    public void setSignedFields(List<String> signedFields) {
        this.signedFields = signedFields;
    }

    public List<String> getIndexFields() {
        return indexFields;
    }

    public void setIndexFields(List<String> indexFields) {
        this.indexFields = indexFields;
    }

    public List<String> getUniqueIndexFields() {
        return uniqueIndexFields;
    }

    public void setUniqueIndexFields(List<String> uniqueIndexFields) {
        this.uniqueIndexFields = uniqueIndexFields;
    }

    public List<String> getSystemFields() {
        return systemFields;
    }

    public void setSystemFields(List<String> systemFields) {
        this.systemFields = systemFields;
    }
}
