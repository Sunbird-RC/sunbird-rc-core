package dev.sunbirdrc.registry.dao.digilocker.pulluriresponse;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "PullURIResponse", namespace = "http://tempuri.org/")
public class PullURIResponse {

    private ResponseUriStatus responseStatus;
    private DocDetails docDetails;

    @XmlElement(name = "ResponseStatus")
    public ResponseUriStatus getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(ResponseUriStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    @XmlElement(name = "DocDetails")
    public DocDetails getDocDetails() {
        return docDetails;
    }

    public void setDocDetails(DocDetails docDetails) {
        this.docDetails = docDetails;
    }

}