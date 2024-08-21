package com.brinvex.fintracker.connector.ibkr.impl.builder;

import com.brinvex.fintracker.api.util.Regex;
import com.brinvex.fintracker.api.util.Validate;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.AssetCategory;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.AssetSubCategory;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.CorporateAction;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.CorporateActionType;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.SecurityIDType;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Accessors(fluent = true, chain = true)
public class CorporateActionBuilder {
    private LocalDate reportDate;
    private AssetCategory assetCategory;
    private AssetSubCategory assetSubCategory;
    private String symbol;
    private CorporateActionType type;
    private String currency;
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
        Validate.notNull(reportDate);
        Validate.notNull(assetCategory);
        Validate.notNull(assetSubCategory);
        Validate.notNullNotBlank(symbol);
        Validate.notNull(type);
        Validate.matches(currency, Regex.CCY);
        Validate.notNull(amount);
        Validate.notNull(value);
        Validate.notNull(quantity);
        Validate.notNull(proceeds);
        Validate.notNull(description);
        Validate.notNullNotBlank(securityID);
        Validate.notNull(securityIDType);
        Validate.notNullNotBlank(figi);
        Validate.notNullNotBlank(isin);
        Validate.notNullNotBlank(listingExchange);
        Validate.notNullNotBlank(issuerCountryCode);
        Validate.notNullNotBlank(extraDateTimeStr);
        Validate.notNullNotBlank(transactionId);
        Validate.notNullNotBlank(actionID);

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
}
