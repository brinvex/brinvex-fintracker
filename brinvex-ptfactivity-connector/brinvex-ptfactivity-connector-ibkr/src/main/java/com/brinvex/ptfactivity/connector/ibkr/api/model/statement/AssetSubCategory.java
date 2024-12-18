package com.brinvex.ptfactivity.connector.ibkr.api.model.statement;

public enum AssetSubCategory {

    COMMON(AssetCategory.STK, "COMMON"),

    ETF(AssetCategory.STK, "ETF"),

    ADR(AssetCategory.STK, "ADR"),

    REIT(AssetCategory.STK, "REIT");

    private final AssetCategory category;

    private final String value;

    public static AssetSubCategory fromValue(String value) {
        for (AssetSubCategory subCategory : AssetSubCategory.values()) {
            if (subCategory.value.equals(value)) {
                return subCategory;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }

    AssetSubCategory(AssetCategory category, String value) {
        this.category = category;
        this.value = value;
    }

    public String value() {
        return value;
    }

    public AssetCategory category() {
        return category;
    }

}
