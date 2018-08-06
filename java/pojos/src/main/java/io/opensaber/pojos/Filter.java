package io.opensaber.pojos;

public class Filter {
	private String property;
	private String operator;
	private Object value;
	
	public String getProperty() {
		return property;
	}
	public void setProperty(String property) {
		this.property = property;
	}
	public String getOperator() {
		return operator;
	}
	public void setOperator(String operator) {
		this.operator = operator;
	}
	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		this.value = value;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((property == null) ? 0 : property.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Filter other = (Filter) obj;
		if (property == null) {
			if (other.property != null)
				return false;
		} else if (!property.equals(other.property))
			return false;
		return true;
	}	
	
}
