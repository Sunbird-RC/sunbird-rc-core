package io.opensaber.pojos;

public class Filter {
	// Denotes the absolute path of the subject
	private String path;

	// The specific attribute being searched for
	private String property;

	// The operator
	private FilterOperators operator;

	// The value that needs to be searched
	private Object value;

	public Filter(String path) {
		this.path = path;
	}

	public Filter(String property, FilterOperators operator, Object value) {
		this.property = property;
		this.operator = operator;
		this.value = value;
	}

	public void setProperty(String property) { this.property = property; }

	public String getProperty() {
		return property;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object object) { this.value = object; }

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public FilterOperators getOperator() { return this.operator;}

	public void setOperator(FilterOperators operator) { this.operator = operator; }
}
