package com.brinvex.ptfactivity.connector.ibkr.internal.builder;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.AssetCategory;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.AssetSubCategory;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.BuySell;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.SecurityIDType;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.TradeConfirm;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.TradeType;
import com.brinvex.java.validation.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@SuppressWarnings("DuplicatedCode")
public class TradeConfirmBuilder {
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
    private LocalDate settleDate;
    private TradeType transactionType;
    private String exchange;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal proceeds;
    private BigDecimal netCash;
    private BigDecimal commission;
    private Currency commissionCurrency;
    private BigDecimal tax;
    private BuySell buySell;
    private String orderID;
    private ZonedDateTime orderTime;
    private String extraDateTimeStr;

    public TradeConfirm build() {
        Assert.notNull(currency);
        Assert.notNull(assetCategory);
        Assert.notNullNotBlank(symbol);
        Assert.notNullNotBlank(description);
        Assert.notNull(securityID);
        Assert.notNull(figi);
        Assert.notNull(isin);
        Assert.notNull(listingExchange);
        Assert.notNullNotBlank(tradeID);
        Assert.notNull(reportDate);
        Assert.notNull(tradeDate);
        Assert.notNull(settleDate);
        Assert.notNull(transactionType);
        Assert.notNullNotBlank(exchange);
        Assert.notNull(quantity);
        Assert.notNull(price);
        Assert.notNull(amount);
        Assert.notNull(proceeds);
        Assert.notNull(netCash);
        Assert.notNull(commission);
        Assert.notNull(commissionCurrency);
        Assert.notNull(tax);
        Assert.notNull(buySell);
        Assert.notNull(buySell);
        Assert.notNullNotBlank(orderID);
        Assert.notNull(orderTime);
        Assert.notNullNotBlank(extraDateTimeStr);

        return new TradeConfirm(
                this.currency,
                this.assetCategory,
                this.assetSubCategory,
                this.symbol,
                this.description,
                this.securityID,
                this.securityIDType,
                this.figi,
                this.isin,
                this.listingExchange,
                this.tradeID,
                this.reportDate,
                this.tradeDate,
                this.settleDate,
                this.transactionType,
                this.exchange,
                this.quantity,
                this.price,
                this.amount,
                this.proceeds,
                this.netCash,
                this.commission,
                this.commissionCurrency,
                this.tax,
                this.buySell,
                this.orderID,
                this.extraDateTimeStr,
                this.orderTime
        );
    }

    public TradeConfirmBuilder currency(Currency currency) {
        this.currency = currency;
        return this;
    }

    public TradeConfirmBuilder assetCategory(AssetCategory assetCategory) {
        this.assetCategory = assetCategory;
        return this;
    }

    public TradeConfirmBuilder assetSubCategory(AssetSubCategory assetSubCategory) {
        this.assetSubCategory = assetSubCategory;
        return this;
    }

    public TradeConfirmBuilder symbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public TradeConfirmBuilder description(String description) {
        this.description = description;
        return this;
    }

    public TradeConfirmBuilder securityID(String securityID) {
        this.securityID = securityID;
        return this;
    }

    public TradeConfirmBuilder securityIDType(SecurityIDType securityIDType) {
        this.securityIDType = securityIDType;
        return this;
    }

    public TradeConfirmBuilder figi(String figi) {
        this.figi = figi;
        return this;
    }

    public TradeConfirmBuilder isin(String isin) {
        this.isin = isin;
        return this;
    }

    public TradeConfirmBuilder listingExchange(String listingExchange) {
        this.listingExchange = listingExchange;
        return this;
    }

    public TradeConfirmBuilder tradeID(String tradeID) {
        this.tradeID = tradeID;
        return this;
    }

    public TradeConfirmBuilder reportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
        return this;
    }

    public TradeConfirmBuilder tradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
        return this;
    }

    public TradeConfirmBuilder settleDate(LocalDate settleDate) {
        this.settleDate = settleDate;
        return this;
    }

    public TradeConfirmBuilder transactionType(TradeType transactionType) {
        this.transactionType = transactionType;
        return this;
    }

    public TradeConfirmBuilder exchange(String exchange) {
        this.exchange = exchange;
        return this;
    }

    public TradeConfirmBuilder quantity(BigDecimal quantity) {
        this.quantity = quantity;
        return this;
    }

    public TradeConfirmBuilder price(BigDecimal price) {
        this.price = price;
        return this;
    }

    public TradeConfirmBuilder amount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public TradeConfirmBuilder proceeds(BigDecimal proceeds) {
        this.proceeds = proceeds;
        return this;
    }

    public TradeConfirmBuilder netCash(BigDecimal netCash) {
        this.netCash = netCash;
        return this;
    }

    public TradeConfirmBuilder commission(BigDecimal commission) {
        this.commission = commission;
        return this;
    }

    public TradeConfirmBuilder commissionCurrency(Currency commissionCurrency) {
        this.commissionCurrency = commissionCurrency;
        return this;
    }

    public TradeConfirmBuilder tax(BigDecimal tax) {
        this.tax = tax;
        return this;
    }

    public TradeConfirmBuilder buySell(BuySell buySell) {
        this.buySell = buySell;
        return this;
    }

    public TradeConfirmBuilder orderID(String orderID) {
        this.orderID = orderID;
        return this;
    }

    public TradeConfirmBuilder orderTime(ZonedDateTime orderTime) {
        this.orderTime = orderTime;
        return this;
    }

    public TradeConfirmBuilder extraDateTimeStr(String extraDateTimeStr) {
        this.extraDateTimeStr = extraDateTimeStr;
        return this;
    }
}
