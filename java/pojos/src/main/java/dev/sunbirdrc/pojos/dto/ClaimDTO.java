package dev.sunbirdrc.pojos.dto;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ClaimDTO {
    private String entity;
    private String entityId;
    private String propertyURI;
    private String notes;
    private String status;
    private String conditions;
    private String attestorEntity;
    private String requestorName;
    private String propertyData;
    private String attestationId;
    private String attestationName;

}
