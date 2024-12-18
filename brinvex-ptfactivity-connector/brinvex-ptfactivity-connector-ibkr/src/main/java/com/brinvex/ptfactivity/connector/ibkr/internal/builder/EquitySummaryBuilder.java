package com.brinvex.ptfactivity.connector.ibkr.internal.builder;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.EquitySummary;
import com.brinvex.java.validation.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;

@SuppressWarnings("DuplicatedCode")
public class EquitySummaryBuilder {
    private LocalDate reportDate;
    private Currency currency;
    private BigDecimal cash;
    private BigDecimal stock;
    private BigDecimal dividendAccruals;
    private BigDecimal interestAccruals;
    private BigDecimal total;

    public EquitySummary build() {
        Assert.notNull(reportDate);
        Assert.notNull(currency);
        Assert.notNull(cash);
        Assert.notNull(stock);
        Assert.notNull(dividendAccruals);
        Assert.notNull(interestAccruals);
        Assert.notNull(total);

        return new EquitySummary(
                this.reportDate,
                this.currency,
                this.cash,
                this.stock,
                this.dividendAccruals,
                this.interestAccruals,
                this.total
        );
    }

    public EquitySummaryBuilder reportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
        return this;
    }

    public EquitySummaryBuilder currency(Currency currency) {
        this.currency = currency;
        return this;
    }

    public EquitySummaryBuilder cash(BigDecimal cash) {
        this.cash = cash;
        return this;
    }

    public EquitySummaryBuilder stock(BigDecimal stock) {
        this.stock = stock;
        return this;
    }

    public EquitySummaryBuilder dividendAccruals(BigDecimal dividendAccruals) {
        this.dividendAccruals = dividendAccruals;
        return this;
    }

    public EquitySummaryBuilder interestAccruals(BigDecimal interestAccruals) {
        this.interestAccruals = interestAccruals;
        return this;
    }

    public EquitySummaryBuilder total(BigDecimal total) {
        this.total = total;
        return this;
    }
}
