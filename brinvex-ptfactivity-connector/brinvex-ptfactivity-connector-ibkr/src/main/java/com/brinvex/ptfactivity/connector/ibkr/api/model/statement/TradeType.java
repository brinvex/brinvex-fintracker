package com.brinvex.ptfactivity.connector.ibkr.api.model.statement;

public enum TradeType {

    EXCH_TRADE("ExchTrade"),

    FRAC_SHARE("FracShare");

    private final String value;

    public static TradeType fromValue(String value) {
        for (TradeType tradeType : TradeType.values()) {
            if (tradeType.value.equals(value)) {
                return tradeType;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }

    TradeType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
