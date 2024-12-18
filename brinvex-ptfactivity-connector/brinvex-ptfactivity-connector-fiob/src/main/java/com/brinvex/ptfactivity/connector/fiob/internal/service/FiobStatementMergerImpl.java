package com.brinvex.ptfactivity.connector.fiob.internal.service;

import com.brinvex.ptfactivity.connector.fiob.api.model.statement.Lang;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.SavingTransaction;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.Statement.SavingTransStatement;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.Statement.TradingTransStatement;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.TradingTransaction;
import com.brinvex.ptfactivity.connector.fiob.api.service.FiobStatementMerger;
import com.brinvex.java.validation.Assert;
import com.brinvex.java.validation.Validate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toCollection;

public class FiobStatementMergerImpl implements FiobStatementMerger {

    @Override
    public Optional<TradingTransStatement> mergeTradingTransStatements(Collection<TradingTransStatement> tradingTransStatements) {
        Validate.notNull("tradingTransStatements collection can not be null");
        if (tradingTransStatements.isEmpty()) {
            return Optional.empty();
        }
        List<TradingTransStatement> sortedStatements = tradingTransStatements
                .stream()
                .sorted(comparing(TradingTransStatement::periodFrom).thenComparing(TradingTransStatement::periodTo))
                .toList();

        String resultAccountId;
        LocalDate resultFromDate;
        LocalDate resultToDate;
        Lang resultLang;
        {
            TradingTransStatement oldestStatement = sortedStatements.getFirst();
            resultLang = oldestStatement.lang();
            resultAccountId = oldestStatement.accountId();
            resultFromDate = oldestStatement.periodFrom();
            resultToDate = oldestStatement.periodTo();
        }
        SequencedSet<TradingTransaction> resultTrans = new LinkedHashSet<>();

        for (TradingTransStatement statement : sortedStatements) {
            String accountId = statement.accountId();
            Assert.equal(accountId, resultAccountId, () -> "Unexpected multiple accounts: %s, %s"
                    .formatted(resultAccountId, accountId));

            Lang lang = statement.lang();
            Assert.equal(lang, resultLang, () -> "Unexpected multiple langs: %s, %s"
                    .formatted(resultLang, lang));

            LocalDate fromDate = statement.periodFrom();
            LocalDate toDate = statement.periodTo();

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

            resultTrans.addAll(statement.transactions());
        }

        return Optional.of(new TradingTransStatement(
                resultAccountId,
                resultFromDate,
                resultToDate,
                new ArrayList<>(resultTrans),
                resultLang
        ));
    }

    @Override
    public Optional<SavingTransStatement> mergeSavingTransStatements(Collection<SavingTransStatement> savingTransStatements) {
        Validate.notNull("savingTransStatements collection can not be null");
        if (savingTransStatements.isEmpty()) {
            return Optional.empty();
        }
        List<SavingTransStatement> sortedStatements = savingTransStatements
                .stream()
                .sorted(comparing(SavingTransStatement::periodFrom).thenComparing(SavingTransStatement::periodTo))
                .toList();

        String resultAccountNumber;
        LocalDate resultPeriodFrom;
        LocalDate resultPeriodTo;
        {
            SavingTransStatement statement0 = sortedStatements.getFirst();
            resultAccountNumber = statement0.accountId();

            resultPeriodFrom = statement0.periodFrom();
            resultPeriodTo = statement0.periodTo();
        }

        Set<SavingTransaction> resultTrans = new LinkedHashSet<>();
        Set<String> resultTranIds = new LinkedHashSet<>();
        for (SavingTransStatement rawTranList : savingTransStatements) {
            LocalDate periodFrom = rawTranList.periodFrom();
            LocalDate periodTo = rawTranList.periodTo();

            String accountNumber = rawTranList.accountId();
            if (!resultAccountNumber.equals(accountNumber)) {
                throw new IllegalStateException("Unexpected multiple accounts: %s, %s"
                        .formatted(resultAccountNumber, accountNumber));
            }

            LocalDate nextPeriodFrom = resultPeriodTo.plusDays(1);
            if (nextPeriodFrom.isBefore(periodFrom)) {
                String message = format("Missing period: '%s - %s', accountNumber=%s",
                        nextPeriodFrom, periodFrom.minusDays(1), resultAccountNumber);
                throw new IllegalStateException(message);
            }
            if (periodTo.isAfter(resultPeriodTo)) {
                resultPeriodTo = periodTo;
            }

            for (SavingTransaction tranListTran : rawTranList.transactions()) {
                boolean idAdded = resultTranIds.add(tranListTran.id());
                if (idAdded) {
                    boolean tranAdded = resultTrans.add(tranListTran);
                    Assert.isTrue(tranAdded);
                }
            }
        }

        return Optional.of(new SavingTransStatement(
                resultAccountNumber,
                resultPeriodFrom,
                resultPeriodTo,
                resultTrans.stream()
                        .sorted(comparing(SavingTransaction::date).thenComparing(SavingTransaction::id))
                        .collect(toCollection(ArrayList::new)))
        );
    }
}
