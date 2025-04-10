package dev.sunbirdrc.pojos;

import lombok.Getter;

@Getter
public enum FilterOperators {
    gte(">="), lte("<="), contains("contains"),
    gt(">"), lt("<"), eq("="), neq("!="),
    between("range"), or("or"),
    startsWith("startsWith"), endsWith("endsWith"), 
    notContains("notContains"), notStartsWith("notStartsWith"), notEndsWith("notEndsWith"),
    queryString("queryString");
    

    private final String value;

    FilterOperators(String value) {
        this.value = value;
    }

    public static FilterOperators get(String name){
        FilterOperators filterOperators = null;
        try {
            filterOperators = FilterOperators.valueOf(name);
        } catch (IllegalArgumentException e){
            for (FilterOperators operators : FilterOperators.values()) {
                if (operators.getValue().equals(name)) {
                    filterOperators = operators;
                }
            }
        }
        return filterOperators;
    }
}
