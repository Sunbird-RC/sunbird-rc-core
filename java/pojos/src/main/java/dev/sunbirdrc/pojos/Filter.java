package dev.sunbirdrc.pojos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
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

}
