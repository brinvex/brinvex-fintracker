package com.brinvex.ptfactivity.connector.ibkr.api.model.statement;

public enum CashTransactionType {

    DEPOSITS_WITHDRAWALS("Deposits/Withdrawals"),

    WITHHOLDING_TAX("Withholding Tax"),

    DIVIDENDS("Dividends"),

    PAYMENT_IN_LIEU_OF_DIVIDENDS("Payment In Lieu Of Dividends"),

    OTHER_FEES("Other Fees"),

    BROKER_INTEREST_PAID("Broker Interest Paid"),

    BROKER_FEES("Broker Fees");

    private final String value;

    public static CashTransactionType fromValue(String value) {
        for (CashTransactionType cashTransactionType : CashTransactionType.values()) {
            if (cashTransactionType.value.equals(value)) {
                return cashTransactionType;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }

    CashTransactionType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
