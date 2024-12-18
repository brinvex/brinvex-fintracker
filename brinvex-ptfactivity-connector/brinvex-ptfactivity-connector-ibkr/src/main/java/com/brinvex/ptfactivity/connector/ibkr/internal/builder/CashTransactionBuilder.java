package com.brinvex.ptfactivity.connector.ibkr.internal.builder;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.AssetCategory;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.AssetSubCategory;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CashTransaction;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CashTransactionType;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.SecurityIDType;
import com.brinvex.java.validation.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;

@SuppressWarnings("DuplicatedCode")
public class CashTransactionBuilder {

    private LocalDate reportDate;
    private Currency currency;
    private AssetCategory assetCategory;
    private BigDecimal fxRateToBase;
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
        Assert.notNull(currency);
        Assert.notNull(fxRateToBase);
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
                fxRateToBase,
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

    public CashTransactionBuilder reportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
        return this;
    }

    public CashTransactionBuilder currency(Currency currency) {
        this.currency = currency;
        return this;
    }

    public CashTransactionBuilder fxRateToBase(BigDecimal fxRateToBase) {
        this.fxRateToBase = fxRateToBase;
        return this;
    }

    public CashTransactionBuilder assetCategory(AssetCategory assetCategory) {
        this.assetCategory = assetCategory;
        return this;
    }

    public CashTransactionBuilder assetSubCategory(AssetSubCategory assetSubCategory) {
        this.assetSubCategory = assetSubCategory;
        return this;
    }

    public CashTransactionBuilder symbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public CashTransactionBuilder description(String description) {
        this.description = description;
        return this;
    }

    public CashTransactionBuilder securityID(String securityID) {
        this.securityID = securityID;
        return this;
    }

    public CashTransactionBuilder securityIDType(SecurityIDType securityIDType) {
        this.securityIDType = securityIDType;
        return this;
    }

    public CashTransactionBuilder figi(String figi) {
        this.figi = figi;
        return this;
    }

    public CashTransactionBuilder isin(String isin) {
        this.isin = isin;
        return this;
    }

    public CashTransactionBuilder listingExchange(String listingExchange) {
        this.listingExchange = listingExchange;
        return this;
    }

    public CashTransactionBuilder type(CashTransactionType type) {
        this.type = type;
        return this;
    }

    public CashTransactionBuilder transactionID(String transactionID) {
        this.transactionID = transactionID;
        return this;
    }

    public CashTransactionBuilder actionID(String actionID) {
        this.actionID = actionID;
        return this;
    }

    public CashTransactionBuilder settleDate(LocalDate settleDate) {
        this.settleDate = settleDate;
        return this;
    }

    public CashTransactionBuilder amount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public CashTransactionBuilder extraDateTimeStr(String extraDateTimeStr) {
        this.extraDateTimeStr = extraDateTimeStr;
        return this;
    }
}
