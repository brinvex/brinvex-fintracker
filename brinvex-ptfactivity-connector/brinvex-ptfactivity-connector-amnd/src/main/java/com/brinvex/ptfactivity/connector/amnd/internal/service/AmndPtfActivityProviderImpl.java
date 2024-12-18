package com.brinvex.ptfactivity.connector.amnd.internal.service;

import com.brinvex.ptfactivity.connector.amnd.api.model.AmndTransStatementDocKey;
import com.brinvex.ptfactivity.connector.amnd.api.model.statement.TransactionStatement;
import com.brinvex.ptfactivity.connector.amnd.api.service.AmndDms;
import com.brinvex.ptfactivity.connector.amnd.api.service.AmndFinTransactionMapper;
import com.brinvex.ptfactivity.connector.amnd.api.service.AmndPtfActivityProvider;
import com.brinvex.ptfactivity.connector.amnd.api.service.AmndStatementParser;
import com.brinvex.ptfactivity.core.api.domain.PtfActivity;
import com.brinvex.ptfactivity.core.api.domain.PtfActivityReq;
import com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType;
import com.brinvex.ptfactivity.core.api.domain.Account;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction.FinTransactionBuilder;
import com.brinvex.java.validation.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;

public class AmndPtfActivityProviderImpl implements AmndPtfActivityProvider {

    private final AmndDms dms;

    private final AmndStatementParser statementParser;

    private final AmndFinTransactionMapper finTransactionMapper;

    public AmndPtfActivityProviderImpl(
            AmndDms dms,
            AmndStatementParser statementParser,
            AmndFinTransactionMapper finTransactionMapper
    ) {
        this.dms = dms;
        this.statementParser = statementParser;
        this.finTransactionMapper = finTransactionMapper;
    }

    @Override
    public boolean supports(PtfActivityReq ptfActivityReq) {
        return ptfActivityReq.providerName().equals("amnd");
    }

    @Override
    public PtfActivity process(PtfActivityReq ptfActivityReq) {
        Assert.isTrue(supports(ptfActivityReq));
        return getPtfProgress(ptfActivityReq.account(), ptfActivityReq.fromDateIncl(), ptfActivityReq.toDateIncl());
    }

    @Override
    public PtfActivity getPtfProgress(Account account, LocalDate fromDateIncl, LocalDate toDateIncl) {
        BigDecimal initialFeeReserve = ofNullable(account.extraProps().get("initialFeeReserve"))
                .filter(not(String::isBlank))
                .map(java.math.BigDecimal::new)
                .orElse(null);
        return getPtfProgress(account.externalId(), initialFeeReserve, fromDateIncl, toDateIncl);
    }


    /**
     * @param initialFeeReserve Represents the initial fee amount paid upfront at the start of the investment.
     *                          This value is reserved to be distributed incrementally across multiple subsequent
     *                          transactions, such as deposits and withdrawals.
     *                          <p>
     *                          Instead of associating the full fee amount with the initial transaction,
     *                          we use this reserve as a base to allocate smaller portions of the fee
     *                          to relevant transactions over time.
     *                          <p>
     *                          This technique helps mitigate the disturbing effect of a large initial fee
     *                          when calculating investment performance.
     */
    private PtfActivity getPtfProgress(String accountId, BigDecimal initialFeeReserve, LocalDate fromDateIncl, LocalDate toDateIncl) {
        LOG.debug("getPtfProgress({}, {}-{})", accountId, fromDateIncl, toDateIncl);

        if (fromDateIncl.isAfter(toDateIncl)) {
            return new PtfActivity(emptyList(), emptyList());
        }

        AmndTransStatementDocKey docKey = dms.getTradingAccountStatementDocKey(accountId);
        byte[] statementContent = dms.getStatementContent(docKey);

        TransactionStatement transStatement = statementParser.parseTrades(statementContent);
        Assert.isTrue(transStatement.accountId().equals(accountId));

        List<FinTransactionBuilder> finTranBuilders = new ArrayList<>(transStatement.trades()
                .stream()
                .map(finTransactionMapper::mapTradeToFinTransactionPair)
                .flatMap(Collection::stream)
                .sorted(comparing(FinTransactionBuilder::date))
                .collect(toMap(FinTransactionBuilder::externalId, identity(), (u, v) -> {
                    throw new IllegalStateException("ExtraID conflict: %s, %s".formatted(u, v));
                }, LinkedHashMap::new))
                .sequencedValues());
        if (!finTranBuilders.isEmpty() && initialFeeReserve != null) {
            int feeReserveSignum = initialFeeReserve.signum();
            if (feeReserveSignum != 0) {
                Assert.isTrue(feeReserveSignum < 0);

                FinTransactionBuilder firstDepositTran = finTranBuilders.get(0);
                FinTransactionBuilder firstBuyTran = finTranBuilders.get(1);
                Assert.isTrue(firstDepositTran.type() == FinTransactionType.DEPOSIT);
                Assert.isTrue(firstBuyTran.type() == FinTransactionType.BUY);

                BigDecimal ignorableFeeAbs = new BigDecimal("0.05");
                List<FinTransactionBuilder> buyTrans = finTranBuilders
                        .stream()
                        .filter(t -> t.type() == FinTransactionType.BUY)
                        .toList();
                List<FinTransactionBuilder> buyTransToAdjust = buyTrans
                        .stream()
                        .takeWhile(t -> t == firstBuyTran || t.fee().abs().compareTo(ignorableFeeAbs) <= 0)
                        .toList();
                buyTrans.stream()
                        .skip(buyTransToAdjust.size())
                        .forEach(t -> Assert.isTrue(t.fee().abs().compareTo(ignorableFeeAbs) > 0));

                Assert.isTrue(firstBuyTran.fee().compareTo(initialFeeReserve) == 0);
                firstBuyTran.fee(ZERO);
                firstBuyTran.netValue(firstBuyTran.grossValue());

                BigDecimal buyTranGrossSum = buyTransToAdjust.stream()
                        .map(FinTransactionBuilder::grossValue)
                        .map(BigDecimal::abs)
                        .reduce(ZERO, BigDecimal::add);
                BigDecimal feeChunkRate = initialFeeReserve.divide(buyTranGrossSum, 10, HALF_UP);

                for (FinTransactionBuilder buyTran : buyTransToAdjust) {
                    Assert.isTrue(buyTran.fee().abs().compareTo(ignorableFeeAbs) <= 0);
                    BigDecimal grossValue = buyTran.grossValue();
                    BigDecimal feeChunk = grossValue.abs().multiply(feeChunkRate);
                    BigDecimal newFee = buyTran.fee().add(feeChunk);
                    buyTran.fee(newFee);
                    buyTran.netValue(grossValue.add(newFee));
                }
            }
        }

        List<FinTransaction> finTrans = finTranBuilders
                .stream()
                .filter(t -> !t.date().isBefore(fromDateIncl) && (toDateIncl == null || !t.date().isAfter(toDateIncl)))
                .map(FinTransactionBuilder::build)
                .toList();

        return new PtfActivity(finTrans, null);
    }

    private static final Logger LOG = LoggerFactory.getLogger(AmndPtfActivityProviderImpl.class);
}
