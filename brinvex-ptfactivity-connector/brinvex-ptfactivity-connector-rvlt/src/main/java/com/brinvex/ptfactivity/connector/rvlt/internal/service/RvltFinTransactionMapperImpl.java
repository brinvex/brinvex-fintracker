package com.brinvex.ptfactivity.connector.rvlt.internal.service;

import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.Transaction;
import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.TransactionType;
import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltFinTransactionMapper;
import com.brinvex.ptfactivity.core.api.domain.Asset;
import com.brinvex.ptfactivity.core.api.domain.enu.AssetType;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType;
import com.brinvex.java.Num;
import com.brinvex.java.validation.Assert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.brinvex.java.NullUtil.coalesce;
import static com.brinvex.java.NullUtil.nullSafe;
import static java.math.BigDecimal.ZERO;

public class RvltFinTransactionMapperImpl implements RvltFinTransactionMapper {

    @Override
    public List<FinTransaction> mapTransactions(List<Transaction> rvltTransactions) {
        List<FinTransaction> resultTrans = new ArrayList<>();

        for (int i = 0, n = rvltTransactions.size(); i < n; i++) {
            Transaction rvltTran = rvltTransactions.get(i);
            TransactionType rvltTranType = rvltTran.type();

            FinTransaction.FinTransactionBuilder tranBuilder = prepareTransaction(rvltTran);
            if (rvltTranType == TransactionType.CASH_TOP_UP) {
                Assert.isTrue(Num.isPositive(rvltTran.value()));
                Assert.isTrue(Num.isNullOrZero(rvltTran.qty()));
                Assert.isTrue(Num.isNullOrZero(rvltTran.fees()));
                Assert.isTrue(Num.isNullOrZero(rvltTran.commission()));
                Assert.isTrue(Num.isNullOrZero(rvltTran.withholdingTax()));
                FinTransaction tran = tranBuilder
                        .type(FinTransactionType.DEPOSIT)
                        .grossValue(rvltTran.value())
                        .netValue(rvltTran.value())
                        .qty(ZERO)
                        .tax(ZERO)
                        .fee(ZERO)
                        .build();
                resultTrans.add(tran);
            } else if (rvltTranType == TransactionType.CASH_WITHDRAWAL) {
                Assert.isTrue(Num.isNegative(rvltTran.value()));
                Assert.isTrue(Num.isNullOrZero(rvltTran.qty()));
                Assert.isTrue(Num.isNullOrZero(rvltTran.fees()));
                Assert.isTrue(Num.isNullOrZero(rvltTran.commission()));
                Assert.isTrue(Num.isNullOrZero(rvltTran.withholdingTax()));
                FinTransaction tran = tranBuilder
                        .type(FinTransactionType.WITHDRAWAL)
                        .grossValue(rvltTran.value())
                        .netValue(rvltTran.value())
                        .qty(ZERO)
                        .tax(ZERO)
                        .fee(ZERO)
                        .build();
                resultTrans.add(tran);
            } else if (rvltTranType == TransactionType.CUSTODY_FEE) {
                Assert.isTrue(Num.isNullOrZero(rvltTran.qty()));
                Assert.isTrue(Num.isNegative(rvltTran.value()));
                Assert.isTrue(Num.isZero(rvltTran.fees()));
                Assert.isTrue(Num.isNullOrZero(rvltTran.commission()));
                Assert.isTrue(Num.isNullOrZero(rvltTran.withholdingTax()));
                FinTransaction tran = tranBuilder
                        .type(FinTransactionType.FEE)
                        .grossValue(ZERO)
                        .netValue(rvltTran.value())
                        .fee(rvltTran.value())
                        .tax(ZERO)
                        .qty(ZERO)
                        .build();
                resultTrans.add(tran);
            } else if (rvltTranType == TransactionType.DIVIDEND) {
                Assert.isTrue(Num.isPositive(rvltTran.value()));
                FinTransaction tran = tranBuilder
                        .type(FinTransactionType.DIVIDEND)
                        .qty(ZERO)
                        .build();
                resultTrans.add(tran);
            } else if (rvltTranType == TransactionType.TRADE_LIMIT || rvltTranType == TransactionType.TRADE_MARKET) {
                Assert.isTrue(Num.isPositive(rvltTran.qty()));
                Assert.isTrue(Num.isNullOrZero(rvltTran.withholdingTax()));
                Assert.isTrue(Num.isPositive(rvltTran.value()));
                BigDecimal fee = calculateRvltFee(rvltTran);
                Assert.isTrue(Num.isNonPositive(fee));
                FinTransaction tran = switch (rvltTran.side()) {
                    case BUY -> tranBuilder
                            .type(FinTransactionType.BUY)
                            .grossValue(rvltTran.value().negate().subtract(fee))
                            .netValue(rvltTran.value().negate())
                            .tax(ZERO)
                            .fee(fee)
                            .build();
                    case SELL -> tranBuilder
                            .type(FinTransactionType.SELL)
                            .qty(rvltTran.qty().negate())
                            .grossValue(rvltTran.value().subtract(fee))
                            .netValue(rvltTran.value())
                            .tax(ZERO)
                            .fee(fee)
                            .build();
                };
                resultTrans.add(tran);
            } else if (rvltTranType == TransactionType.STOCK_SPLIT) {
                Assert.isTrue(Num.isZero(rvltTran.value()));
                Assert.isTrue(Num.isNullOrZero(rvltTran.fees()));
                Assert.isTrue(Num.isNullOrZero(rvltTran.commission()));
                Assert.isTrue(Num.isNullOrZero(rvltTran.withholdingTax()));
                FinTransaction tran = tranBuilder
                        .type(FinTransactionType.TRANSFORMATION)
                        .grossValue(ZERO)
                        .netValue(ZERO)
                        .fee(rvltTran.value())
                        .tax(ZERO)
                        .build();
                resultTrans.add(tran);
            } else if (rvltTranType == TransactionType.SPINOFF) {
                Assert.isTrue(Num.isZero(rvltTran.value()));
                Assert.isTrue(Num.isZero(rvltTran.fees()));
                Assert.isTrue(Num.isZero(rvltTran.commission()));
                Assert.isTrue(Num.isNullOrZero(rvltTran.withholdingTax()));

                String tranGroupId = constructRevtTranId(rvltTran);
                String parentTranExtraId = tranGroupId + "/SPINOFF_PARENT";
                String childTranExtraId = tranGroupId + "/SPINOFF_CHILD";

                Transaction next1RevtTran = i == n - 1 ? null : rvltTransactions.get(i + 1);

                if (next1RevtTran != null && next1RevtTran.type().equals(TransactionType.SPINOFF)) {
                    Assert.isTrue(i < n - 2);
                    Transaction next2RevtTran = rvltTransactions.get(i + 2);
                    Assert.isTrue(next2RevtTran.type().equals(TransactionType.SPINOFF));
                    Assert.isTrue(next1RevtTran.symbol().equals(rvltTran.symbol()));
                    Assert.isTrue(next1RevtTran.qty().equals(rvltTran.qty()));
                    FinTransaction parentTran = tranBuilder
                            .type(FinTransactionType.TRANSFORMATION)
                            .externalId(parentTranExtraId)
                            .grossValue(ZERO)
                            .netValue(ZERO)
                            .qty(ZERO)
                            .fee(ZERO)
                            .tax(ZERO)
                            .groupId(tranGroupId)
                            .build();
                    resultTrans.add(parentTran);
                    i = i + 2;
                }

                Assert.isTrue(rvltTran.qty().compareTo(ZERO) > 0);

                FinTransaction childTran = tranBuilder
                        .type(FinTransactionType.TRANSFORMATION)
                        .externalId(childTranExtraId)
                        .grossValue(ZERO)
                        .netValue(ZERO)
                        .qty(rvltTran.qty())
                        .fee(ZERO)
                        .tax(ZERO)
                        .groupId(tranGroupId)
                        .build();
                resultTrans.add(childTran);
            } else if (rvltTranType == TransactionType.MERGER) {
                Assert.isTrue(Num.isZero(rvltTran.value()));
                Assert.isTrue(Num.isZero(rvltTran.fees()));
                Assert.isTrue(Num.isZero(rvltTran.commission()));
                Assert.isTrue(Num.isNullOrZero(rvltTran.withholdingTax()));

                String tranGroupId = constructRevtTranId(rvltTran);
                String parentTranExtraId = tranGroupId + "/MERGER_PARENT";
                String childTranExtraId = tranGroupId + "/MERGER_CHILD";

                Assert.notNull(i < n - 1);
                Transaction next1RevtTran = rvltTransactions.get(i + 1);
                Assert.notNull(next1RevtTran);
                Assert.isTrue(next1RevtTran.type().equals(TransactionType.MERGER));
                Assert.isTrue(next1RevtTran.value().compareTo(ZERO) == 0);
                Assert.isTrue(next1RevtTran.fees().compareTo(ZERO) == 0);
                Assert.isFalse(next1RevtTran.symbol().equals(rvltTran.symbol()));

                Assert.isTrue(rvltTran.value().compareTo(ZERO) == 0);
                Assert.isTrue(rvltTran.fees().compareTo(ZERO) == 0);
                Assert.isTrue(rvltTran.qty().compareTo(ZERO) > 0);
                Assert.isTrue(next1RevtTran.qty().compareTo(ZERO) < 0);

                FinTransaction parentTran = prepareTransaction(next1RevtTran)
                        .type(FinTransactionType.TRANSFORMATION)
                        .externalId(parentTranExtraId)
                        .groupId(tranGroupId)
                        .externalType(TransactionType.MERGER + "/PARENT")
                        .grossValue(ZERO)
                        .tax(ZERO)
                        .build();
                resultTrans.add(parentTran);
                i = i + 1;

                FinTransaction childTran = tranBuilder
                        .type(FinTransactionType.TRANSFORMATION)
                        .externalId(childTranExtraId)
                        .groupId(tranGroupId)
                        .externalType(TransactionType.MERGER + "/CHILD")
                        .grossValue(ZERO)
                        .tax(ZERO)
                        .build();

                resultTrans.add(childTran);
            } else {
                throw new IllegalStateException("%s".formatted(rvltTran));
            }
        }

        return resultTrans;
    }

    private FinTransaction.FinTransactionBuilder prepareTransaction(Transaction rvltTran) {
        FinTransaction.FinTransactionBuilder tranBuilder = FinTransaction.builder();
        tranBuilder.date(rvltTran.date().toLocalDate());
        if (rvltTran.symbol() != null) {
            tranBuilder.asset(Asset.builder()
                    .type(AssetType.STOCK)
                    .country(rvltTran.country())
                    .symbol(rvltTran.symbol())
                    .name(rvltTran.securityName())
                    .isin(rvltTran.isin())
                    .build());
        }
        tranBuilder.qty(rvltTran.qty());
        tranBuilder.ccy(rvltTran.ccy());
        tranBuilder.price(rvltTran.price());
        tranBuilder.grossValue(rvltTran.grossAmount());
        tranBuilder.netValue(rvltTran.value());
        tranBuilder.tax(nullSafe(rvltTran.withholdingTax(), BigDecimal::negate));
        tranBuilder.fee(calculateRvltFee(rvltTran));
        tranBuilder.externalType(rvltTran.type().toString());
        tranBuilder.externalId(constructRevtTranId(rvltTran));
        tranBuilder.groupId(null);

        return tranBuilder;
    }

    private BigDecimal calculateRvltFee(Transaction rvltTran) {
        return coalesce(rvltTran.commission(), ZERO).add(coalesce(rvltTran.fees(), ZERO)).negate();
    }

    private String constructRevtTranId(Transaction rvltTran) {
        return "%s/%s/%s/%s/%s/%s/%s".formatted(
                rvltTran.date(),
                rvltTran.type(),
                rvltTran.isin(),
                rvltTran.symbol(),
                rvltTran.qty(),
                rvltTran.price(),
                rvltTran.value()
        );
    }
}
