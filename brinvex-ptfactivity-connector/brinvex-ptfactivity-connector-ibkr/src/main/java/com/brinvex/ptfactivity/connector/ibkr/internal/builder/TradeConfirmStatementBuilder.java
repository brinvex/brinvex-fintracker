package com.brinvex.ptfactivity.connector.ibkr.internal.builder;

import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.FlexStatement;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.TradeConfirm;
import com.brinvex.java.validation.Assert;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

public class TradeConfirmStatementBuilder {
    private String accountId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private ZonedDateTime whenGenerated;
    private Collection<TradeConfirm> tradeConfirmations;

    public FlexStatement.TradeConfirmStatement build() {
        Assert.notNullNotBlank(accountId);
        Assert.notNull(fromDate);
        Assert.notNull(toDate);
        Assert.notNull(whenGenerated);
        Assert.notNull(tradeConfirmations);
        return new FlexStatement.TradeConfirmStatement(
                accountId,
                fromDate,
                toDate,
                whenGenerated,
                List.copyOf(tradeConfirmations)
        );
    }

    public TradeConfirmStatementBuilder accountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public TradeConfirmStatementBuilder fromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
        return this;
    }

    public TradeConfirmStatementBuilder toDate(LocalDate toDate) {
        this.toDate = toDate;
        return this;
    }

    public TradeConfirmStatementBuilder whenGenerated(ZonedDateTime whenGenerated) {
        this.whenGenerated = whenGenerated;
        return this;
    }

    public TradeConfirmStatementBuilder tradeConfirmations(Collection<TradeConfirm> tradeConfirmations) {
        this.tradeConfirmations = tradeConfirmations;
        return this;
    }
}
