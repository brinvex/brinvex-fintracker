package com.brinvex.fintracker.connector.ibkr.impl.builder;

import com.brinvex.fintracker.api.util.Regex;
import com.brinvex.util.java.validation.Assert;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.AssetCategory;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.AssetSubCategory;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.BuySell;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.SecurityIDType;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.TradeConfirm;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.TradeType;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@SuppressWarnings("DuplicatedCode")
@Setter
@Accessors(fluent = true, chain = true)
public class TradeConfirmBuilder {
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
    private LocalDate settleDate;
    private TradeType transactionType;
    private String exchange;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal proceeds;
    private BigDecimal netCash;
    private BigDecimal commission;
    private String commissionCurrency;
    private BigDecimal tax;
    private BuySell buySell;
    private String orderID;
    private ZonedDateTime orderTime;
    private String extraDateTimeStr;

    public TradeConfirm build() {
        Assert.matches(currency, Regex.Pattern.CCY.pattern());
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
}
