package com.brinvex.ptfactivity.connector.ibkr.api.model.statement;


import com.brinvex.finance.types.enu.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CorporateAction(
        LocalDate reportDate,
        AssetCategory assetCategory,
        AssetSubCategory assetSubCategory,
        String symbol,
        CorporateActionType type,
        Currency currency,
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
) {
}
