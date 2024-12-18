package com.brinvex.ptfactivity.connector.ibkr.internal.builder;

import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CashTransaction;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CorporateAction;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.EquitySummary;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.FlexStatement;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.Trade;
import com.brinvex.java.validation.Assert;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("DuplicatedCode")
public class ActivityStatementBuilder {
    private String accountId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private ZonedDateTime whenGenerated;
    private Collection<CashTransaction> cashTransactions;
    private Collection<Trade> trades;
    private Collection<CorporateAction> corporateActions;
    private Collection<EquitySummary> equitySummaries;

    public FlexStatement.ActivityStatement build() {
        Assert.notNullNotBlank(accountId);
        Assert.notNull(fromDate);
        Assert.notNull(toDate);
        Assert.notNull(cashTransactions);
        Assert.notNull(trades);
        Assert.notNull(corporateActions);
        Assert.notNull(equitySummaries);

        return new FlexStatement.ActivityStatement(
                accountId,
                fromDate,
                toDate,
                whenGenerated,
                List.copyOf(cashTransactions),
                List.copyOf(trades),
                List.copyOf(corporateActions),
                List.copyOf(equitySummaries)
        );
    }

    public ActivityStatementBuilder accountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public ActivityStatementBuilder fromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
        return this;
    }

    public ActivityStatementBuilder toDate(LocalDate toDate) {
        this.toDate = toDate;
        return this;
    }

    public ActivityStatementBuilder whenGenerated(ZonedDateTime whenGenerated) {
        this.whenGenerated = whenGenerated;
        return this;
    }

    public ActivityStatementBuilder cashTransactions(Collection<CashTransaction> cashTransactions) {
        this.cashTransactions = cashTransactions;
        return this;
    }

    public ActivityStatementBuilder trades(Collection<Trade> trades) {
        this.trades = trades;
        return this;
    }

    public ActivityStatementBuilder corporateActions(Collection<CorporateAction> corporateActions) {
        this.corporateActions = corporateActions;
        return this;
    }

    public ActivityStatementBuilder equitySummaries(Collection<EquitySummary> equitySummaries) {
        this.equitySummaries = equitySummaries;
        return this;
    }
}
