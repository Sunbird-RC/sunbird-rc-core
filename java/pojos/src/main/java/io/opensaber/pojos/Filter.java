package io.opensaber.pojos;

import java.util.List;

public class Filter {
	private List<String> path;
	private String subject;
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
	
	public List<String> getPath() {
		return path;
	}
	public void setPath(List<String> path) {
		this.path = path;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((property == null) ? 0 : property.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
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
		if ((property == null && other.property != null) || !property.equals(other.property)) {
				return false;
		}
		if ((subject == null && other.subject != null) || !subject.equals(other.subject)) {
				return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return "Filter [path=" + path + ", property=" + property + ", operator=" + operator + ", value=" + value + "]";
	}
	
	
}
