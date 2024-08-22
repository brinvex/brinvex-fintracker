package com.brinvex.fintracker.connector.ibkr.impl.builder;

import com.brinvex.fintracker.api.util.Regex;
import com.brinvex.fintracker.common.impl.Validate;
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
        Validate.matches(currency, Regex.CCY);
        Validate.notNull(assetCategory);
        Validate.notNullNotBlank(symbol);
        Validate.notNullNotBlank(description);
        Validate.notNull(securityID);
        Validate.notNull(figi);
        Validate.notNull(isin);
        Validate.notNull(listingExchange);
        Validate.notNull(tradeID);
        Validate.notNull(reportDate);
        Validate.notNull(tradeDate);
        Validate.notNull(settleDateTarget);
        Validate.notNull(transactionType);
        Validate.notNullNotBlank(exchange);
        Validate.notNull(quantity);
        Validate.notNull(tradePrice);
        Validate.notNull(tradeMoney);
        Validate.notNull(proceeds);
        Validate.notNull(taxes);
        Validate.notNull(ibCommission);
        Validate.notNull(ibCommissionCurrency);
        Validate.notNull(netCash);
        Validate.notNull(cost);
        Validate.notNull(buySell);
        Validate.notNullNotBlank(transactionID);
        Validate.notNullNotBlank(ibOrderID);
        Validate.notNullNotBlank(extraDateTimeStr);

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
