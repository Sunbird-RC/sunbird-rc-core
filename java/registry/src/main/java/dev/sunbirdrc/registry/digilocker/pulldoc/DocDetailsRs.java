package dev.sunbirdrc.registry.digilocker.pulldoc;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DocDetails")
public class DocDetailsRs {

    private Object docContent;
    private Object dataContent;

    @XmlElement(name = "DocContent")
    public Object getDocContent() {
        return docContent;
    }

    public void setDocContent(Object docContent) {
        this.docContent = docContent;
    }

    @XmlElement(name = "DataContent")
    public Object getDataContent() {
        return dataContent;
    }

    public void setDataContent(Object dataContent) {
        this.dataContent = dataContent;
    }


}
