package com.brinvex.ptfactivity.connector.ibkr.internal.builder;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.AssetCategory;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.AssetSubCategory;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.BuySell;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.SecurityIDType;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.Trade;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.TradeType;
import com.brinvex.java.validation.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@SuppressWarnings("DuplicatedCode")
public class TradeBuilder {
    private Currency currency;
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
    private Currency ibCommissionCurrency;
    private BigDecimal netCash;
    private BigDecimal cost;
    private BuySell buySell;
    private String transactionID;
    private String ibOrderID;
    private String extraDateTimeStr;
    private ZonedDateTime orderTime;

    public Trade build() {
        Assert.notNull(currency);
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

    public TradeBuilder currency(Currency currency) {
        this.currency = currency;
        return this;
    }

    public TradeBuilder assetCategory(AssetCategory assetCategory) {
        this.assetCategory = assetCategory;
        return this;
    }

    public TradeBuilder assetSubCategory(AssetSubCategory assetSubCategory) {
        this.assetSubCategory = assetSubCategory;
        return this;
    }

    public TradeBuilder symbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public TradeBuilder description(String description) {
        this.description = description;
        return this;
    }

    public TradeBuilder securityID(String securityID) {
        this.securityID = securityID;
        return this;
    }

    public TradeBuilder securityIDType(SecurityIDType securityIDType) {
        this.securityIDType = securityIDType;
        return this;
    }

    public TradeBuilder figi(String figi) {
        this.figi = figi;
        return this;
    }

    public TradeBuilder isin(String isin) {
        this.isin = isin;
        return this;
    }

    public TradeBuilder listingExchange(String listingExchange) {
        this.listingExchange = listingExchange;
        return this;
    }

    public TradeBuilder tradeID(String tradeID) {
        this.tradeID = tradeID;
        return this;
    }

    public TradeBuilder reportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
        return this;
    }

    public TradeBuilder tradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
        return this;
    }

    public TradeBuilder settleDateTarget(LocalDate settleDateTarget) {
        this.settleDateTarget = settleDateTarget;
        return this;
    }

    public TradeBuilder transactionType(TradeType transactionType) {
        this.transactionType = transactionType;
        return this;
    }

    public TradeBuilder exchange(String exchange) {
        this.exchange = exchange;
        return this;
    }

    public TradeBuilder quantity(BigDecimal quantity) {
        this.quantity = quantity;
        return this;
    }

    public TradeBuilder tradePrice(BigDecimal tradePrice) {
        this.tradePrice = tradePrice;
        return this;
    }

    public TradeBuilder tradeMoney(BigDecimal tradeMoney) {
        this.tradeMoney = tradeMoney;
        return this;
    }

    public TradeBuilder proceeds(BigDecimal proceeds) {
        this.proceeds = proceeds;
        return this;
    }

    public TradeBuilder taxes(BigDecimal taxes) {
        this.taxes = taxes;
        return this;
    }

    public TradeBuilder ibCommission(BigDecimal ibCommission) {
        this.ibCommission = ibCommission;
        return this;
    }

    public TradeBuilder ibCommissionCurrency(Currency ibCommissionCurrency) {
        this.ibCommissionCurrency = ibCommissionCurrency;
        return this;
    }

    public TradeBuilder netCash(BigDecimal netCash) {
        this.netCash = netCash;
        return this;
    }

    public TradeBuilder cost(BigDecimal cost) {
        this.cost = cost;
        return this;
    }

    public TradeBuilder buySell(BuySell buySell) {
        this.buySell = buySell;
        return this;
    }

    public TradeBuilder transactionID(String transactionID) {
        this.transactionID = transactionID;
        return this;
    }

    public TradeBuilder ibOrderID(String ibOrderID) {
        this.ibOrderID = ibOrderID;
        return this;
    }

    public TradeBuilder extraDateTimeStr(String extraDateTimeStr) {
        this.extraDateTimeStr = extraDateTimeStr;
        return this;
    }

    public TradeBuilder orderTime(ZonedDateTime orderTime) {
        this.orderTime = orderTime;
        return this;
    }
}
