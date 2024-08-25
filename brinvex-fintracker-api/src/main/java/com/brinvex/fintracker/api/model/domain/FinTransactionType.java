package com.brinvex.fintracker.api.model.domain;

public enum FinTransactionType {

    DEPOSIT(FinTransactionCategory.EXTERNAL_FLOW),

    WITHDRAWAL(FinTransactionCategory.EXTERNAL_FLOW),

    BUY(FinTransactionCategory.TRADE),

    SELL(FinTransactionCategory.TRADE),

    FX_BUY(FinTransactionCategory.TRADE),

    FX_SELL(FinTransactionCategory.TRADE),

    DIVIDEND(FinTransactionCategory.INTERNAL_FLOW),

    INTEREST(FinTransactionCategory.INTERNAL_FLOW),

    OTHER_INCOME(FinTransactionCategory.INTERNAL_FLOW),

    FEE(FinTransactionCategory.INTERNAL_FLOW),

    TAX(FinTransactionCategory.INTERNAL_FLOW),

    OTHER_OUTCOME(FinTransactionCategory.INTERNAL_FLOW),

    TRANSFORMATION(FinTransactionCategory.OTHER),

    OTHER(FinTransactionCategory.OTHER);

    private final FinTransactionCategory category;

    FinTransactionType(FinTransactionCategory category) {
        this.category = category;
    }

    public FinTransactionCategory getCategory() {
        return category;
    }
}
