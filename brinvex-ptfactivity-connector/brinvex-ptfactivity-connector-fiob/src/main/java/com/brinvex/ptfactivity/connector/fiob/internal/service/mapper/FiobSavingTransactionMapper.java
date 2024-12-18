package com.brinvex.ptfactivity.connector.fiob.internal.service.mapper;

import com.brinvex.ptfactivity.connector.fiob.api.model.statement.SavingTransaction;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType;
import com.brinvex.java.validation.Assert;

import java.util.ArrayList;
import java.util.List;

import static java.math.BigDecimal.ZERO;

public class FiobSavingTransactionMapper {

    @SuppressWarnings("SpellCheckingInspection")
    public List<FinTransaction> mapTransactions(List<SavingTransaction> rawTrans) {
        List<FinTransaction> trans = new ArrayList<>();

        for (int i = 0, rawTransSize = rawTrans.size(); i < rawTransSize; i++) {
            SavingTransaction rawTran = rawTrans.get(i);
            try {
                FinTransaction.FinTransactionBuilder newTran = FinTransaction.builder();
                newTran.type(detectTranType(rawTran));
                newTran.externalId(rawTran.id());
                newTran.date(rawTran.date());
                newTran.ccy(rawTran.ccy());
                newTran.grossValue(rawTran.volume());
                newTran.netValue(newTran.grossValue());
                newTran.qty(ZERO);
                newTran.fee(ZERO);
                newTran.tax(ZERO);
                newTran.settleDate(rawTran.date());

                if (FinTransactionType.INTEREST.equals(newTran.type()) && i < rawTransSize - 1) {
                    SavingTransaction nextRawTran = rawTrans.get(i + 1);
                    if (rawTran.date().isEqual(nextRawTran.date())) {
                        String nextRawType = nextRawTran.type();
                        FinTransactionType nextTranType = detectTranType(nextRawTran);
                        if (FinTransactionType.TAX.equals(nextTranType) && "Odvod daně z úroků".equals(nextRawType)) {
                            Assert.equal(newTran.ccy(), nextRawTran.ccy());
                            newTran.tax(nextRawTran.volume());
                            newTran.reconcileNetValue();
                            i++;
                        }
                    }
                }
                trans.add(newTran.build());
            } catch (Exception e) {
                throw new IllegalStateException("%s - rawTran=%s".formatted(i + 1, rawTran), e);
            }
        }

        return trans;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private FinTransactionType detectTranType(SavingTransaction tran) {
        String rawType = tran.type();
        if ("Bezhotovostní příjem".equals(rawType)) {
            return FinTransactionType.DEPOSIT;
        }
        if ("Příjem převodem uvnitř banky".equals(rawType)) {
            return FinTransactionType.DEPOSIT;
        }
        if ("Platba kartou".equals(rawType)) {
            return FinTransactionType.WITHDRAWAL;
        }
        if ("Bezhotovostní platba".equals(rawType)) {
            return FinTransactionType.WITHDRAWAL;
        }
        if ("Platba převodem uvnitř banky".equals(rawType)) {
            return FinTransactionType.WITHDRAWAL;
        }
        if ("Připsaný úrok".equals(rawType)) {
            return FinTransactionType.INTEREST;
        }
        if ("Odvod daně z úroků".equals(rawType)) {
            return FinTransactionType.TAX;
        }
        throw new IllegalArgumentException(String.valueOf(tran));
    }

}