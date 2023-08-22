package dev.sunbirdrc.registry.digilocker.pulldoc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class PullDocRequest {
    @XmlElement(name = "DocDetails", required = true)
    protected DocDetailsType docDetails;
    protected String ver;
    protected String ts;
    protected String txn;
    protected String orgId;
    protected String format;

    public String getKeyhash() {
        return keyhash;
    }

    public void setKeyhash(String keyhash) {
        this.keyhash = keyhash;
    }

    public String getHmac() {
        return hmac;
    }

    public void setHmac(String hmac) {
        this.hmac = hmac;
    }

    protected String keyhash;
    protected String hmac;

    public DocDetailsType getDocDetails() {
        return docDetails;
    }

    public void setDocDetails(DocDetailsType value) {
        this.docDetails = value;
    }

    public String getVer() {
        return ver;
    }

    public void setVer(String value) {
        this.ver = value;
    }

    public String getTs() {
        return ts;
    }

    public void setTs(String value) {
        this.ts = value;
    }

    public String getTxn() {
        return txn;
    }

    public void setTxn(String value) {
        this.txn = value;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String value) {
        this.orgId = value;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String value) {
        this.format = value;
    }
}


