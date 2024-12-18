package com.brinvex.ptfactivity.connector.ibkr.api.model.statement;

public enum BuySell {

    BUY("BUY"),

    SELL("SELL");

    private final String value;

    public static BuySell fromValue(String value) {
        for (BuySell tradeType : BuySell.values()) {
            if (tradeType.value.equals(value)) {
                return tradeType;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }

    BuySell(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
