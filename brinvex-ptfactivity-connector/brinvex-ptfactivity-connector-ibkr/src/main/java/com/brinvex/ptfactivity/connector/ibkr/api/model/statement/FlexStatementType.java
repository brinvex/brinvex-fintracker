package com.brinvex.ptfactivity.connector.ibkr.api.model.statement;

public enum FlexStatementType {

    /**
     * Activity
     */
    ACT("AF"),

    /**
     * TradeConfirmation
     */
    TC("TCF");

    private final String value;

    public static FlexStatementType fromValue(String value) {
        for (FlexStatementType type : FlexStatementType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }

    FlexStatementType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
