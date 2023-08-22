package dev.sunbirdrc.registry.util;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DocDetails")
public class DocDetails {
    private String uID;
    private String digiLockerId;
    private String mobile;
    private String email;
    private String name;
    private String finalYearRollNo;
    private String docType;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    private String entityName;


    public String getuID() {
        return uID;
    }

    public void setuID(String uID) {
        this.uID = uID;
    }

    public String getDigiLockerId() {
        return digiLockerId;
    }

    public void setDigiLockerId(String digiLockerId) {
        this.digiLockerId = digiLockerId;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFinalYearRollNo() {
        return finalYearRollNo;
    }

    public void setFinalYearRollNo(String finalYearRollNo) {
        this.finalYearRollNo = finalYearRollNo;
    }



    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

}


