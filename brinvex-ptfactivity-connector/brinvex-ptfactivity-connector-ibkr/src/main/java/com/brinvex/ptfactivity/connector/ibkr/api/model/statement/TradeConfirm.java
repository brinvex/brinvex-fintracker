package com.brinvex.ptfactivity.connector.ibkr.api.model.statement;

import com.brinvex.finance.types.enu.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

public record TradeConfirm(
        Currency currency,
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
        Currency commissionCurrency,
        BigDecimal tax,
        BuySell buySell,
        String orderID,
        String extraDateTimeStr,
        ZonedDateTime orderTime
) {
}
