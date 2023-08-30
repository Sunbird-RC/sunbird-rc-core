package dev.sunbirdrc.registry.digilocker.pulldoc;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="ResponseStatus")
public class ResponseStatus {

    private String status;
    private String ts;
    private String txn;

    @XmlAttribute(name = "Status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @XmlAttribute(name = "ts")
    public String getTs() {
        return ts;
    }

    public void setTs(String ts) {
        this.ts = ts;
    }

    @XmlAttribute(name = "txn")
    public String getTxn() {
        return txn;
    }

    public void setTxn(String txn) {
        this.txn = txn;
    }


}
