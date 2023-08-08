package dev.sunbirdrc.registry.util;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "PullURIRequest")
public class PullURIRequest {
    // Add other fields as needed
    private DocDetails docDetails;

    @XmlElement(name = "DocDetails")
    public void setDocDetails( DocDetails docDetails) {
        this.docDetails = docDetails;
    }

    public  DocDetails getDocDetails() {
        return docDetails;
    }
}

