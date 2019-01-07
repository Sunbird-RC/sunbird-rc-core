package io.opensaber.pojos;

public class Filter {
	// Denotes the absolute path of the subject
	private String path;

	// The specific attribute being searched for
	private String property;

	// The operator
	private String operator;

	// The value that needs to be searched
	private String value;

	public Filter(String path) {
		this.path = path;
	}

	public Filter(String property, String operator, String value) {
		this.property = property;
		//this.operator = operator;
		this.value = value;
	}

	public void setProperty(String property) { this.property = property; }

	public String getProperty() {
		return property;
	}

	/*public String getOperator() {
		return operator;
	}*/

	public String getValue() {
		return value;
	}

	public void setValue(String value) { this.value = value; }

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getOperator() { return this.operator;}

	public void setOperator(String operator) { this.operator = operator; }
}
