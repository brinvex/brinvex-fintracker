package com.brinvex.ptfactivity.connector.rvlt.internal.service;

import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.Transaction;
import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.TransactionType;
import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltFinTransactionMerger;
import com.brinvex.java.Num;
import com.brinvex.java.validation.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.brinvex.java.NullUtil.nullSafe;
import static java.math.BigDecimal.ZERO;
import static java.util.stream.Collectors.groupingBy;

public class RvltFinTransactionMergerImpl implements RvltFinTransactionMerger {

    @SuppressWarnings("DuplicatedCode")
    @Override
    public List<Transaction> mergeTransactions(List<Transaction> taTransactions, List<Transaction> pnlTransactions) {

        Map<LocalDate, Map<String, List<Transaction>>> pnlTransMap = pnlTransactions
                .stream()
                .collect(groupingBy(t -> t.date().toLocalDate(), groupingBy(Transaction::symbol)));

        Map<Object, Transaction> resultTrans = new LinkedHashMap<>();
        for (Transaction taTran : taTransactions) {

            if (taTran.type() == TransactionType.DIVIDEND) {
                List<Transaction> pairedPnlTrans = nullSafe(pnlTransMap.get(taTran.date().toLocalDate()), datePnlTrans -> datePnlTrans.get(taTran.symbol()));
                int pairedPnlSize = pairedPnlTrans.size();
                if (pairedPnlSize == 1) {
                    Transaction pairedPnlTran = pairedPnlTrans.getFirst();
                    Transaction resultTran = taTran.toBuilder()
                            .securityName(pairedPnlTran.securityName())
                            .isin(pairedPnlTran.isin())
                            .country(pairedPnlTran.country())
                            .grossAmount(pairedPnlTran.grossAmount())
                            .withholdingTax(pairedPnlTran.withholdingTax())
                            .value(pairedPnlTran.value())
                            .fees(pairedPnlTran.fees())
                            .commission(pairedPnlTran.commission())
                            .build();
                    Object resultTranKey = constructTransKey(resultTran);
                    Transaction duplTran = resultTrans.put(resultTranKey, resultTran);
                    Assert.isNull(duplTran);
                } else if (pairedPnlSize > 1) {
                    BigDecimal pairedPnlTranValuesSum = pairedPnlTrans.stream().map(Transaction::value).reduce(ZERO, BigDecimal::add);
                    if (taTran.value().compareTo(pairedPnlTranValuesSum) != 0) {
                        throw new IllegalStateException(
                                "Trading Account Statement transaction value must be equal to the sum of paired P&L Statement transactions, " +
                                "given: %s, %s, %s, %s".formatted(
                                        taTran.value(),
                                        pairedPnlTranValuesSum,
                                        taTran,
                                        pairedPnlTrans
                                ));
                    }
                    for (Transaction pairedPnlTran : pairedPnlTrans) {
                        Transaction resultTran = taTran.toBuilder()
                                .securityName(pairedPnlTran.securityName())
                                .isin(pairedPnlTran.isin())
                                .country(pairedPnlTran.country())
                                .grossAmount(pairedPnlTran.grossAmount())
                                .withholdingTax(pairedPnlTran.withholdingTax())
                                .value(pairedPnlTran.value())
                                .fees(pairedPnlTran.fees())
                                .commission(pairedPnlTran.commission())
                                .build();
                        Object resultTranKey = constructTransKey(resultTran);
                        Transaction duplTran = resultTrans.put(resultTranKey, resultTran);
                        Assert.isNull(duplTran);
                    }
                } else {
                    throw new IllegalStateException("Trading Account Statement transaction not found among P&L Statement transactions: %s".formatted(taTran));
                }
            } else {
                Object taTranKey = constructTransKey(taTran);
                Transaction duplTran = resultTrans.put(taTranKey, taTran);
                Assert.isNull(duplTran);
            }
        }

        return new ArrayList<>(resultTrans.values());
    }

    private Object constructTransKey(Transaction transaction) {
        return Arrays.asList(
                transaction.type(),
                transaction.date(),
                transaction.symbol(),
                Num.setScale8(transaction.qty()),
                Num.setScale2(transaction.price()),
                transaction.side(),
                Num.setScale2(transaction.value()),
                Num.setScale2(transaction.fees()),
                Num.setScale2(transaction.commission())
        );
    }

    private Object constructDividTransKey(Transaction transaction) {
        return Arrays.asList(
                transaction.date().toLocalDate(),
                transaction.symbol(),
                Num.setScale(transaction.value(), 2)
        );
    }

    private Object constructMultiDividTransKey(Transaction transaction) {
        return List.of(
                transaction.date().toLocalDate(),
                transaction.symbol()
        );
    }

}
