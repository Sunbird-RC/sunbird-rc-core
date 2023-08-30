package dev.sunbirdrc.registry.digilocker.pulldoc;

import javax.xml.bind.annotation.XmlValue;

public class DataContent {

    private Object content;

    @XmlValue
    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

}