package io.opensaber.pojos;

public enum FilterOperators {
    gte(">="), lte("<="), contains("contains"),
    gt(">"), lt("<"), eq("="),
    between("range");

    private String value;

    FilterOperators(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
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
