package com.brinvex.ptfactivity.connector.rvlt.internal.service;

import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltDms;
import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltPtfActivityProvider;
import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltFinTransactionMapper;
import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltFinTransactionMerger;
import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltStatementParser;
import com.brinvex.finance.types.vo.DateAmount;
import com.brinvex.ptfactivity.connector.rvlt.api.model.RvltDocKey.PnlStatementDocKey;
import com.brinvex.ptfactivity.connector.rvlt.api.model.RvltDocKey.TradingAccountStatementDocKey;
import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.PnlStatement;
import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.TradingAccountStatement;
import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.Transaction;
import com.brinvex.ptfactivity.core.api.domain.Account;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import com.brinvex.java.validation.Assert;
import com.brinvex.ptfactivity.core.api.domain.PtfActivity;
import com.brinvex.ptfactivity.core.api.domain.PtfActivityReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedMap;
import java.util.SequencedSet;

import static com.brinvex.java.DateUtil.isLastDayOfMonth;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class RvltPtfActivityProviderImpl implements RvltPtfActivityProvider {

    private final RvltDms dms;

    private final RvltStatementParser statementParser;

    private final RvltFinTransactionMerger finTransactionMerger;

    private final RvltFinTransactionMapper finTransactionMapper;

    public RvltPtfActivityProviderImpl(
            RvltDms dms,
            RvltStatementParser statementParser,
            RvltFinTransactionMerger finTransactionMerger,
            RvltFinTransactionMapper finTransactionMapper
    ) {
        this.dms = dms;
        this.statementParser = statementParser;
        this.finTransactionMerger = finTransactionMerger;
        this.finTransactionMapper = finTransactionMapper;
    }

    @Override
    public boolean supports(PtfActivityReq ptfActivityReq) {
        return ptfActivityReq.providerName().equals("rvlt");
    }

    @Override
    public PtfActivity process(PtfActivityReq ptfActivityReq) {
        Assert.isTrue(supports(ptfActivityReq));
        return getPtfProgress(ptfActivityReq.account(), ptfActivityReq.fromDateIncl(), ptfActivityReq.toDateIncl());
    }

    @Override
    public PtfActivity getPtfProgress(Account account, LocalDate fromDateIncl, LocalDate toDateIncl) {
        LOG.debug("getPtfProgress({}, {}-{})", account, fromDateIncl, toDateIncl);

        String accountNumber = account.externalId();
        String accountName = account.name();

        LocalDate adjToDateIncl = isLastDayOfMonth(toDateIncl) ? toDateIncl : toDateIncl.withDayOfMonth(1).minusDays(1);
        if (fromDateIncl.isAfter(adjToDateIncl)) {
            return new PtfActivity(emptyList(), emptyList());
        }

        List<TradingAccountStatementDocKey> taDocKeys = dms.getTradingAccountStatementDocKeys(
                accountNumber, fromDateIncl, adjToDateIncl);
        if (taDocKeys.isEmpty()) {
            return new PtfActivity(emptyList(), emptyList());
        }
        List<PnlStatementDocKey> pnlStatementDocKeys = dms.getPnlStatementDocKeys(accountNumber, fromDateIncl, adjToDateIncl);
        Assert.isTrue(pnlStatementDocKeys.size() == 1);

        PnlStatement pnlStatement = statementParser.parsePnlStatement(dms.getStatementContent(pnlStatementDocKeys.getFirst()));
        Assert.isTrue(accountNumber.equals(pnlStatement.accountNumber()));
        Assert.isTrue(accountName == null || accountName.equals(pnlStatement.accountName()));
        List<Transaction> pnlTransactions = pnlStatement.transactions();

        SequencedMap<LocalDate, BigDecimal> navs = new LinkedHashMap<>();
        SequencedSet<FinTransaction> finTransactions = new LinkedHashSet<>();
        LocalDate prevTaPeriodEnd = null;
        for (TradingAccountStatementDocKey taDocKey : taDocKeys) {
            TradingAccountStatement taStatement = statementParser.parseTradingAccountStatement(dms.getStatementContent(taDocKey));
            Assert.isTrue(accountNumber.equals(taStatement.accountNumber()));
            Assert.isTrue(accountName == null || accountName.equals(taStatement.accountName()));
            Assert.equal(account.ccy(), taStatement.ccy());

            LocalDate taPeriodStart = taStatement.periodStartIncl();
            LocalDate taPeriodEnd = taStatement.periodEndIncl();
            Assert.isTrue(taPeriodEnd.getDayOfMonth() == 1);
            Assert.isTrue(!taPeriodStart.plusMonths(1).isBefore(taPeriodEnd));
            if (prevTaPeriodEnd != null) {
                Assert.isTrue(prevTaPeriodEnd.plusMonths(1).isEqual(taPeriodEnd));
            }

            navs.put(taPeriodEnd.minusDays(1), taStatement.endValue());

            /*
            The Trading Account Statement for a month includes transactions
            that occurred on the first day of the following month (i.e., on the period end date)
            These transactions are presented again in the statement for the next month,
            so we can ignore them while processing the current month. However, before ignoring them,
            we ensure they indeed took place on the first day of the following month and not later.
            */
            List<List<Transaction>> taTrans = taStatement.transactions()
                    .stream()
                    .dropWhile(t -> t.date().toLocalDate().isBefore(fromDateIncl))
                    .takeWhile(t -> !t.date().toLocalDate().isAfter(adjToDateIncl))
                    .collect(groupingBy(t -> t.date().toLocalDate().withDayOfMonth(1), LinkedHashMap::new, toList()))
                    .values()
                    .stream()
                    .toList();
            int taTransGroupSize = taTrans.size();
            List<Transaction> properTaTrans;
            if (taTransGroupSize == 0) {
                properTaTrans = emptyList();
            } else if (taTransGroupSize == 1) {
                properTaTrans = taTrans.getFirst();
            } else if (taTransGroupSize == 2) {
                properTaTrans = taTrans.getFirst();
                List<Transaction> firstDayNextMonthTaTrans = taTrans.get(1);
                for (Transaction firstDayNextMonthTaTran : firstDayNextMonthTaTrans) {
                    Assert.isTrue(firstDayNextMonthTaTran.date().toLocalDate().isEqual(taPeriodEnd));
                }
            } else {
                throw new IllegalStateException("Trading Account Statement contain transaction dates for 3 or more months, %s".formatted(taDocKey));
            }

            if (!properTaTrans.isEmpty()) {
                Assert.isTrue(!properTaTrans.getFirst().date().toLocalDate().isBefore(taPeriodStart));

                List<Transaction> mergedTrans = finTransactionMerger.mergeTransactions(properTaTrans, pnlTransactions);
                List<FinTransaction> mappedTrans = finTransactionMapper.mapTransactions(mergedTrans);

                finTransactions.addAll(mappedTrans);
            }

            prevTaPeriodEnd = taPeriodEnd;
        }

        return new PtfActivity(
                finTransactions,
                navs.entrySet()
                        .stream()
                        .dropWhile(e -> e.getKey().isBefore(fromDateIncl))
                        .takeWhile(e -> !e.getKey().isAfter(adjToDateIncl))
                        .map(DateAmount::new)
                        .toList()
        );
    }

    private static final Logger LOG = LoggerFactory.getLogger(RvltPtfActivityProviderImpl.class);

}
