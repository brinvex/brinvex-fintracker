package com.brinvex.fintracker.connector.ibkr.api.model.statement;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

public record Trade(
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
        String tradeID,
        LocalDate reportDate,
        LocalDate tradeDate,
        LocalDate settleDateTarget,
        TradeType transactionType,
        String exchange,
        BigDecimal quantity,
        BigDecimal tradePrice,
        BigDecimal tradeMoney,
        BigDecimal proceeds,
        BigDecimal taxes,
        BigDecimal ibCommission,
        String ibCommissionCurrency,
        BigDecimal netCash,
        BigDecimal cost,
        BuySell buySell,
        String transactionID,
        String ibOrderID,
        String extraDateTimeStr,
        ZonedDateTime orderTime
) implements Serializable {
}
