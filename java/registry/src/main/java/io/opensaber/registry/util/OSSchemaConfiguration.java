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
    
}
