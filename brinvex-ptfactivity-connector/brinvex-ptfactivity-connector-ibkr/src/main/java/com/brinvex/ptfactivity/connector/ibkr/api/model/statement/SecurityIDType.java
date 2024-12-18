package com.brinvex.ptfactivity.connector.ibkr.api.model.statement;

public enum SecurityIDType {

    ISIN("ISIN");

    private final String value;

    public static SecurityIDType fromValue(String value) {
        for (SecurityIDType type : SecurityIDType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }

    SecurityIDType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
