package com.brinvex.fintracker.connector.ibkr.api.model.statement;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CashTransaction(
        LocalDate reportDate,
        String currency,
        AssetCategory assetCategory,
        AssetSubCategory assetSubCategory,
        String symbol,
        String description,
        String securityID,
        SecurityIDType securityIDType,
        String figi,
        String isin,
        String listingExchange,
        CashTransactionType type,
        String transactionID,
        String actionID,
        LocalDate settleDate,
        BigDecimal amount,
        String extraDateTimeStr
) implements Serializable {
}
