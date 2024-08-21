package com.brinvex.fintracker.connector.ibkr.impl.builder;

import com.brinvex.fintracker.api.util.Regex;
import com.brinvex.fintracker.api.util.Validate;
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
        Validate.matches(currency, Regex.CCY);
        Validate.notNull(assetCategory);
        Validate.notNullNotBlank(symbol);
        Validate.notNullNotBlank(description);
        Validate.notNull(securityID);
        Validate.notNull(figi);
        Validate.notNull(isin);
        Validate.notNull(listingExchange);
        Validate.notNullNotBlank(tradeID);
        Validate.notNull(reportDate);
        Validate.notNull(tradeDate);
        Validate.notNull(settleDate);
        Validate.notNull(transactionType);
        Validate.notNullNotBlank(exchange);
        Validate.notNull(quantity);
        Validate.notNull(price);
        Validate.notNull(amount);
        Validate.notNull(proceeds);
        Validate.notNull(netCash);
        Validate.notNull(commission);
        Validate.notNull(commissionCurrency);
        Validate.notNull(tax);
        Validate.notNull(buySell);
        Validate.notNull(buySell);
        Validate.notNullNotBlank(orderID);
        Validate.notNull(orderTime);
        Validate.notNullNotBlank(extraDateTimeStr);

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
