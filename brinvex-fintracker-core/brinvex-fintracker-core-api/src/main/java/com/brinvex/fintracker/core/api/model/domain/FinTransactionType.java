package com.brinvex.fintracker.core.api.model.domain;

public enum FinTransactionType {

    DEPOSIT(FinTransactionFlowType.EXTERNAL),

    WITHDRAWAL(FinTransactionFlowType.EXTERNAL),

    BUY(FinTransactionFlowType.INTERNAL),

    SELL(FinTransactionFlowType.INTERNAL),

    FX_BUY(FinTransactionFlowType.INTERNAL),

    FX_SELL(FinTransactionFlowType.INTERNAL),

    CASH_DIVIDEND(FinTransactionFlowType.INTERNAL),

    INTEREST(FinTransactionFlowType.INTERNAL),

    FEE(FinTransactionFlowType.INTERNAL),

    TAX(FinTransactionFlowType.INTERNAL),

    TRANSFORMATION(FinTransactionFlowType.INTERNAL),

    OTHER_INTERNAL_FLOW(FinTransactionFlowType.INTERNAL);

    private final FinTransactionFlowType flowType;

    FinTransactionType(FinTransactionFlowType flowType) {
        this.flowType = flowType;
    }

    public FinTransactionFlowType flowType() {
        return flowType;
    }
}
