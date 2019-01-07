package io.opensaber.pojos;

public enum FilterOperators {
    gte("gte"), lte("lte"), contains("contains"), equals("eq"),
    gt(">"), lt("<"), eq("=");

    private String name;

    FilterOperators(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
