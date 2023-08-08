package dev.sunbirdrc.registry.digilocker.pulluriresponse;


import javax.xml.bind.annotation.XmlAttribute;


public class PhotoBean {
    String format = "";
    String textContent = "";

    @XmlAttribute(name = "format")
    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @XmlAttribute(name = "TextContent")
    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }


}
