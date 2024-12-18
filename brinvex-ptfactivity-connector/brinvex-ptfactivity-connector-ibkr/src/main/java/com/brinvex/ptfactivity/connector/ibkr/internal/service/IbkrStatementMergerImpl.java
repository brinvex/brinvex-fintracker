package com.brinvex.ptfactivity.connector.ibkr.internal.service;

import com.brinvex.java.validation.Assert;
import com.brinvex.java.validation.Validate;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CashTransaction;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CorporateAction;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.EquitySummary;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.FlexStatement.ActivityStatement;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.Trade;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrStatementMerger;
import com.brinvex.ptfactivity.connector.ibkr.internal.builder.ActivityStatementBuilder;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;

import static java.util.Comparator.comparing;

public class IbkrStatementMergerImpl implements IbkrStatementMerger {

    @Override
    public Optional<ActivityStatement> mergeActivityStatements(Collection<ActivityStatement> activityStatements) {
        Validate.notNull("activityStatements collection can not be null");
        if (activityStatements.isEmpty()) {
            return Optional.empty();
        }
        List<ActivityStatement> sortedStatements = activityStatements
                .stream()
                .sorted(comparing(ActivityStatement::fromDate).thenComparing(ActivityStatement::toDate))
                .toList();

        String resultAccountId;
        LocalDate resultFromDate;
        LocalDate resultToDate;
        {
            ActivityStatement oldestStatement = sortedStatements.getFirst();
            resultAccountId = oldestStatement.accountId();
            resultFromDate = oldestStatement.fromDate();
            resultToDate = oldestStatement.toDate();
        }
        SequencedSet<CashTransaction> cashTrans = new LinkedHashSet<>();
        SequencedSet<Trade> trades = new LinkedHashSet<>();
        SequencedSet<CorporateAction> corpActions = new LinkedHashSet<>();
        SequencedSet<EquitySummary> equitySummaries = new LinkedHashSet<>();

        for (ActivityStatement statement : sortedStatements) {
            String accountId = statement.accountId();
            Assert.equal(accountId, resultAccountId, () -> "Unexpected multiple accounts: %s, %s"
                    .formatted(resultAccountId, accountId));

            LocalDate fromDate = statement.fromDate();
            LocalDate toDate = statement.toDate();

            LocalDate expectedFromDate = resultToDate.plusDays(1);
            if (expectedFromDate.isBefore(fromDate)) {
                throw new IllegalStateException(
                        "Missing period: <%s, %s>, externalId=%s"
                                .formatted(expectedFromDate, fromDate.minusDays(1), resultAccountId)
                );
            }

            if (toDate.isAfter(resultToDate)) {
                resultToDate = toDate;
            }

            cashTrans.addAll(statement.cashTransactions());
            trades.addAll(statement.trades());
            corpActions.addAll(statement.corporateActions());
            equitySummaries.addAll(statement.equitySummaries());
        }

        return Optional.of(new ActivityStatementBuilder()
                .accountId(resultAccountId)
                .fromDate(resultFromDate)
                .toDate(resultToDate)
                .cashTransactions(cashTrans)
                .trades(trades)
                .corporateActions(corpActions)
                .equitySummaries(equitySummaries)
                .build());
    }
}
