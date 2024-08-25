package com.brinvex.fintracker.connector.ibkr.impl.builder;

import com.brinvex.util.java.validation.Assert;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.FlexStatement;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.TradeConfirm;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

@Setter
@Accessors(fluent = true, chain = true)
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

}
