package dev.sunbirdrc.registry.digilocker.pulluriresponse;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DocDetails {
	private IssuedTo issuedTo;
	private String uri;
	private Object docContent;
	private Object dataContent;
	public IssuedTo getIssuedTo() {
		return issuedTo;
	}

	public void setIssuedTo(IssuedTo issuedTo) {
		this.issuedTo = issuedTo;
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