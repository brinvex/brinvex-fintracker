package com.brinvex.fintracker.connector.ibkr.impl.builder;

import com.brinvex.fintracker.common.impl.Validate;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.CashTransaction;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.CorporateAction;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.EquitySummary;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.FlexStatement;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.Trade;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("DuplicatedCode")
@Setter
@Accessors(fluent = true, chain = true)
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
        Validate.notNullNotBlank(accountId);
        Validate.notNull(fromDate);
        Validate.notNull(toDate);
        Validate.notNull(whenGenerated);
        Validate.notNull(cashTransactions);
        Validate.notNull(trades);
        Validate.notNull(corporateActions);
        Validate.notNull(equitySummaries);

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

}
