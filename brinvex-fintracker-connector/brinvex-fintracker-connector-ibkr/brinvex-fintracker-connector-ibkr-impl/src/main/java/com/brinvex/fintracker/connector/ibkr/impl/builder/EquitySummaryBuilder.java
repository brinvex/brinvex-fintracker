package com.brinvex.fintracker.connector.ibkr.impl.builder;

import com.brinvex.fintracker.api.util.Regex;
import com.brinvex.util.java.validation.Assert;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.EquitySummary;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Accessors(fluent = true, chain = true)
public class EquitySummaryBuilder {
    private LocalDate reportDate;
    private String currency;
    private BigDecimal cash;
    private BigDecimal stock;
    private BigDecimal dividendAccruals;
    private BigDecimal interestAccruals;
    private BigDecimal total;

    public EquitySummary build() {
        Assert.notNull(reportDate);
        Assert.matches(currency, Regex.CCY.pattern());
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
}
