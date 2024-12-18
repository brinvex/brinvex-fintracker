package com.brinvex.ptfactivity.connector.ibkr.api.model.statement;

public enum CorporateActionType {

    MERGED_ACQUISITION("TC"),

    SPIN_OFF("SO"),

    SPLIT("FS");

    private final String value;

    public static CorporateActionType fromValue(String value) {
        for (CorporateActionType cashTransactionType : CorporateActionType.values()) {
            if (cashTransactionType.value.equals(value)) {
                return cashTransactionType;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }

    CorporateActionType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
