package com.brinvex.ptfactivity.connector.ibkr.internal.builder;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.AssetCategory;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.AssetSubCategory;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CorporateAction;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CorporateActionType;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.SecurityIDType;
import com.brinvex.java.validation.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CorporateActionBuilder {
    private LocalDate reportDate;
    private AssetCategory assetCategory;
    private AssetSubCategory assetSubCategory;
    private String symbol;
    private CorporateActionType type;
    private Currency currency;
    private BigDecimal amount;
    private BigDecimal value;
    private BigDecimal quantity;
    private BigDecimal proceeds;
    private String description;
    private String securityID;
    private SecurityIDType securityIDType;
    private String figi;
    private String isin;
    private String listingExchange;
    private String issuerCountryCode;
    private String extraDateTimeStr;
    private String transactionId;
    private String actionID;


    public CorporateAction build() {
        Assert.notNull(reportDate);
        Assert.notNull(assetCategory);
        Assert.notNull(assetSubCategory);
        Assert.notNullNotBlank(symbol);
        Assert.notNull(type);
        Assert.notNull(currency);
        Assert.notNull(amount);
        Assert.notNull(value);
        Assert.notNull(quantity);
        Assert.notNull(proceeds);
        Assert.notNull(description);
        Assert.notNullNotBlank(securityID);
        Assert.notNull(securityIDType);
        Assert.notNullNotBlank(figi);
        Assert.notNullNotBlank(isin);
        Assert.notNullNotBlank(listingExchange);
        Assert.notNullNotBlank(issuerCountryCode);
        Assert.notNullNotBlank(extraDateTimeStr);
        Assert.notNullNotBlank(transactionId);
        Assert.notNullNotBlank(actionID);

        return new CorporateAction(
                this.reportDate,
                this.assetCategory,
                this.assetSubCategory,
                this.symbol,
                this.type,
                this.currency,
                this.amount,
                this.value,
                this.quantity,
                this.proceeds,
                this.description,
                this.securityID,
                this.securityIDType,
                this.figi,
                this.isin,
                this.listingExchange,
                this.issuerCountryCode,
                this.extraDateTimeStr,
                this.transactionId,
                this.actionID);
    }

    public CorporateActionBuilder reportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
        return this;
    }

    public CorporateActionBuilder assetCategory(AssetCategory assetCategory) {
        this.assetCategory = assetCategory;
        return this;
    }

    public CorporateActionBuilder assetSubCategory(AssetSubCategory assetSubCategory) {
        this.assetSubCategory = assetSubCategory;
        return this;
    }

    public CorporateActionBuilder symbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public CorporateActionBuilder type(CorporateActionType type) {
        this.type = type;
        return this;
    }

    public CorporateActionBuilder currency(Currency currency) {
        this.currency = currency;
        return this;
    }

    public CorporateActionBuilder amount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public CorporateActionBuilder value(BigDecimal value) {
        this.value = value;
        return this;
    }

    public CorporateActionBuilder quantity(BigDecimal quantity) {
        this.quantity = quantity;
        return this;
    }

    public CorporateActionBuilder proceeds(BigDecimal proceeds) {
        this.proceeds = proceeds;
        return this;
    }

    public CorporateActionBuilder description(String description) {
        this.description = description;
        return this;
    }

    public CorporateActionBuilder securityID(String securityID) {
        this.securityID = securityID;
        return this;
    }

    public CorporateActionBuilder securityIDType(SecurityIDType securityIDType) {
        this.securityIDType = securityIDType;
        return this;
    }

    public CorporateActionBuilder figi(String figi) {
        this.figi = figi;
        return this;
    }

    public CorporateActionBuilder isin(String isin) {
        this.isin = isin;
        return this;
    }

    public CorporateActionBuilder listingExchange(String listingExchange) {
        this.listingExchange = listingExchange;
        return this;
    }

    public CorporateActionBuilder issuerCountryCode(String issuerCountryCode) {
        this.issuerCountryCode = issuerCountryCode;
        return this;
    }

    public CorporateActionBuilder extraDateTimeStr(String extraDateTimeStr) {
        this.extraDateTimeStr = extraDateTimeStr;
        return this;
    }

    public CorporateActionBuilder transactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public CorporateActionBuilder actionID(String actionID) {
        this.actionID = actionID;
        return this;
    }
}
