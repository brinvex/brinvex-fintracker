package com.brinvex.fintracker.connector.ibkr.api.model.statement;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

public record TradeConfirm(
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
        LocalDate settleDate,
        TradeType transactionType,
        String exchange,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal amount,
        BigDecimal proceeds,
        BigDecimal netCash,
        BigDecimal commission,
        String commissionCurrency,
        BigDecimal tax,
        BuySell buySell,
        String orderID,
        String extraDateTimeStr,
        ZonedDateTime orderTime
) implements Serializable {
}
