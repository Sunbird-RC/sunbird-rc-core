package dev.sunbirdrc.pojos.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ConsentDTO {
    private String entityName;
    private String entityId;
    private String requestorName;
    private String requestorId;
    private Map<String, String> consentFieldsPath;
    private String consentExpiryTime;
    private List<String> osOwner;
}
