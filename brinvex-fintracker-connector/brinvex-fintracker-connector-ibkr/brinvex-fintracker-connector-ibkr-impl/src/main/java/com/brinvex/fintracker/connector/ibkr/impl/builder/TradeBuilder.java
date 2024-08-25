package com.brinvex.fintracker.connector.ibkr.impl.builder;

import com.brinvex.fintracker.api.util.Regex;
import com.brinvex.util.java.validation.Assert;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.AssetCategory;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.AssetSubCategory;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.BuySell;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.SecurityIDType;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.Trade;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.TradeType;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@SuppressWarnings("DuplicatedCode")
@Setter
@Accessors(fluent = true, chain = true)
public class TradeBuilder {
    private String currency;
    private AssetCategory assetCategory;
    private AssetSubCategory assetSubCategory;
    private String symbol;
    private String description;
    private String securityID;
    private SecurityIDType securityIDType;
    private String figi;
    private String isin;
    private String listingExchange;
    private String tradeID;
    private LocalDate reportDate;
    private LocalDate tradeDate;
    private LocalDate settleDateTarget;
    private TradeType transactionType;
    private String exchange;
    private BigDecimal quantity;
    private BigDecimal tradePrice;
    private BigDecimal tradeMoney;
    private BigDecimal proceeds;
    private BigDecimal taxes;
    private BigDecimal ibCommission;
    private String ibCommissionCurrency;
    private BigDecimal netCash;
    private BigDecimal cost;
    private BuySell buySell;
    private String transactionID;
    private String ibOrderID;
    private String extraDateTimeStr;
    private ZonedDateTime orderTime;

    public Trade build() {
        Assert.matches(currency, Regex.CCY.pattern());
        Assert.notNull(assetCategory);
        Assert.notNullNotBlank(symbol);
        Assert.notNullNotBlank(description);
        Assert.notNull(securityID);
        Assert.notNull(figi);
        Assert.notNull(isin);
        Assert.notNull(listingExchange);
        Assert.notNull(tradeID);
        Assert.notNull(reportDate);
        Assert.notNull(tradeDate);
        Assert.notNull(settleDateTarget);
        Assert.notNull(transactionType);
        Assert.notNullNotBlank(exchange);
        Assert.notNull(quantity);
        Assert.notNull(tradePrice);
        Assert.notNull(tradeMoney);
        Assert.notNull(proceeds);
        Assert.notNull(taxes);
        Assert.notNull(ibCommission);
        Assert.notNull(ibCommissionCurrency);
        Assert.notNull(netCash);
        Assert.notNull(buySell);
        Assert.notNullNotBlank(ibOrderID);
        Assert.notNullNotBlank(extraDateTimeStr);

        return new Trade(
                currency,
                assetCategory,
                assetSubCategory,
                symbol,
                description,
                securityID,
                securityIDType,
                figi,
                isin,
                listingExchange,
                tradeID,
                reportDate,
                tradeDate,
                settleDateTarget,
                transactionType,
                exchange,
                quantity,
                tradePrice,
                tradeMoney,
                proceeds,
                taxes,
                ibCommission,
                ibCommissionCurrency,
                netCash,
                cost,
                buySell,
                transactionID,
                ibOrderID,
                extraDateTimeStr,
                orderTime
        );
    }
}
