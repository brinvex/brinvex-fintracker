package com.brinvex.fintracker.api.model.domain;

public enum AssetType {

    CASH(AssetCategory.CASH),

    STOCK(AssetCategory.INSTRUMENT),

    ETF(AssetCategory.INSTRUMENT),

    FUND(AssetCategory.INSTRUMENT),

    BOND(AssetCategory.INSTRUMENT);

    private final AssetCategory category;

    AssetType(AssetCategory category) {
        this.category = category;
    }

    public AssetCategory category() {
        return category;
    }
}
