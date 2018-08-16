package io.opensaber.pojos;

import java.util.List;

public class SearchQuery {
	
	private List<Filter> filters;
	private int limit;
	private int offset;
	private List<String> fields;
	private String type;
	private String typeIRI;
	
	public List<Filter> getFilters() {
		return filters;
	}
	public void setFilters(List<Filter> filters) {
		this.filters = filters;
	}
	public int getLimit() {
		return limit;
	}
	public void setLimit(int limit) {
		this.limit = limit;
	}
	public int getOffset() {
		return offset;
	}
	public void setOffset(int offset) {
		this.offset = offset;
	}
	public List<String> getFields() {
		return fields;
	}
	public void setFields(List<String> fields) {
		this.fields = fields;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getTypeIRI() {
		return typeIRI;
	}
	public void setTypeIRI(String typeIRI) {
		this.typeIRI = typeIRI;
	}

}
