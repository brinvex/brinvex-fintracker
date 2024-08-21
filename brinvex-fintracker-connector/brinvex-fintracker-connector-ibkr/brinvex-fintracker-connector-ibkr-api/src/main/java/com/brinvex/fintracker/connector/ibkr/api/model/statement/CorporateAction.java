package com.brinvex.fintracker.connector.ibkr.api.model.statement;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CorporateAction(
        LocalDate reportDate,
        AssetCategory assetCategory,
        AssetSubCategory assetSubCategory,
        String symbol,
        CorporateActionType type,
        String currency,
        BigDecimal amount,
        BigDecimal value,
        BigDecimal quantity,
        BigDecimal proceeds,
        String description,
        String securityID,
        SecurityIDType securityIDType,
        String figi,
        String isin,
        String listingExchange,
        String issuerCountryCode,
        String extraDateTimeStr,
        String transactionId,
        String actionID
) implements Serializable {
}
