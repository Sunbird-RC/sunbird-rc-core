package dev.sunbirdrc.registry.dao.digilocker.pulldoc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DocDetailsType", propOrder = {
        "uri",
        "digiLockerId"
})
public class DocDetailsType {

    protected String uri;

    protected String digiLockerId;

    public String getUri() {
        return uri;
    }

    public void setUri(String value) {
        this.uri = value;
    }

    public String getDigiLockerId() {
        return digiLockerId;
    }

    public void setDigiLockerId(String value) {
        this.digiLockerId = value;
    }
}

