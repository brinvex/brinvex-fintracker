package com.brinvex.ptfactivity.connector.ibkr.api.model.statement;

public enum AssetCategory {

    STK("STK"),

    CASH("CASH");

    private final String value;

    public static AssetCategory fromValue(String value) {
        for (AssetCategory category : AssetCategory.values()) {
            if (category.value.equals(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }

    AssetCategory(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
