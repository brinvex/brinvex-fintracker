package com.brinvex.fintracker.connector.ibkr.impl.builder;

import com.brinvex.fintracker.core.api.util.Regex;
import com.brinvex.util.java.validation.Assert;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.AssetCategory;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.AssetSubCategory;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.CashTransaction;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.CashTransactionType;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.SecurityIDType;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Accessors(fluent = true, chain = true)
public class CashTransactionBuilder {

    private LocalDate reportDate;
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
    private CashTransactionType type;
    private String transactionID;
    private String actionID;
    private LocalDate settleDate;
    private BigDecimal amount;
    private String extraDateTimeStr;


    public CashTransaction build() {
        Assert.notNull(reportDate);
        Assert.matches(currency, Regex.Pattern.CCY.pattern());
        Assert.notNull(symbol);
        Assert.notNull(description);
        Assert.notNull(securityID);
        Assert.notNull(figi);
        Assert.notNull(isin);
        Assert.notNull(listingExchange);
        Assert.notNull(type);
        Assert.notNullNotBlank(transactionID);
        Assert.notNull(actionID);
        Assert.notNull(settleDate);
        Assert.notNull(amount);
        Assert.notNullNotBlank(extraDateTimeStr);

        return new CashTransaction(
                reportDate,
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
                type,
                transactionID,
                actionID,
                settleDate,
                amount,
                extraDateTimeStr
        );
    }
}
