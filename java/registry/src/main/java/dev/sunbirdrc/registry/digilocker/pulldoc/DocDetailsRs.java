package dev.sunbirdrc.registry.digilocker.pulldoc;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DocDetails")
public class DocDetailsRs {
    private String docType;

    private String digiLockerId;

    private String uid;

    private String fullName;

    private String dob;

    private String trackingId;
    private String mobile;

    private String udf1;

    private String uri;

    private Object docContent;

    private Object dataContent;

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getDigiLockerId() {
        return digiLockerId;
    }

    public void setDigiLockerId(String digiLockerId) {
        this.digiLockerId = digiLockerId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getUdf1() {
        return udf1;
    }

    public void setUdf1(String udf1) {
        this.udf1 = udf1;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Object getDocContent() {
        return docContent;
    }

    public void setDocContent(Object docContent) {
        this.docContent = docContent;
    }

    public Object getDataContent() {
        return dataContent;
    }

    public void setDataContent(Object dataContent) {
        this.dataContent = dataContent;
    }

}

