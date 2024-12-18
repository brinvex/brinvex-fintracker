package test.com.brinvex.ptfactivity.connector.rvlt.impl.service;


import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltPtfActivityProvider;
import com.brinvex.finance.types.vo.DateAmount;
import com.brinvex.ptfactivity.connector.rvlt.api.RvltModule;
import com.brinvex.ptfactivity.core.api.domain.PtfActivity;
import com.brinvex.ptfactivity.core.api.facade.ValidatorFacade;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType;
import com.brinvex.ptfactivity.core.api.domain.constraints.fintransaction.FinTransactionConstraints;
import com.brinvex.ptfactivity.testsupport.SimplePtf;
import com.brinvex.ptfactivity.testsupport.TestContext;
import com.brinvex.ptfactivity.testsupport.ObfuscationUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;

import static com.brinvex.java.collection.Collectors.toLinkedMap;
import static java.math.BigDecimal.ZERO;
import static java.time.LocalDate.parse;
import static java.util.function.Function.identity;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RvltPtfActivityTest extends RvltBaseTest {

    @SuppressWarnings({"SequencedCollectionMethodCanBeUsed", "SpellCheckingInspection"})
    @EnabledIf("account1IsNotNull")
    @Test
    void ptfProgress1() {
        TestContext testCtx = this.testCtx.withDmsWorkspace("rvlt-dms-stable");
        RvltModule rvltModule = testCtx.get(RvltModule.class);
        ValidatorFacade validator = testCtx.validator();
        RvltPtfActivityProvider rvltPtfProgressProvider = rvltModule.ptfProgressProvider();
        PtfActivity ptfActivity = rvltPtfProgressProvider.getPtfProgress(account1, parse("2020-02-04"), parse("2024-12-01"));
        validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
        LinkedHashMap<LocalDate, DateAmount> navs = ptfActivity.netAssetValues().stream().collect(toLinkedMap(DateAmount::date, identity()));
        List<LocalDate> navDates = List.copyOf(navs.keySet());
        assertEquals(new DateAmount("2020-02-29", "878.11"), navs.get(navDates.get(0)).with(ObfuscationUtil.remainder1000));
        assertEquals(new DateAmount("2020-03-31", "355.59"), navs.get(navDates.get(1)).with(ObfuscationUtil.remainder1000));
        assertEquals(new DateAmount("2024-08-31", "257.53"), navs.get(parse("2024-08-31")).with(ObfuscationUtil.remainder1000));
        assertEquals(new DateAmount("2024-09-30", "674.40"), navs.get(parse("2024-09-30")).with(ObfuscationUtil.remainder1000));

        {
            LocalDate tranDate1 = parse("2024-12-01");
            SimplePtf ptf = new SimplePtf(ptfActivity.transactions().stream().takeWhile(t -> t.date().isBefore(tranDate1)).toList());
            assertEquals("3.598291", ptf.getHoldingQty("US", "VTRS").toPlainString());
            assertEquals("0.00000000", ptf.getHoldingQty("US", "MRO").toPlainString());
            assertEquals("72.00000006", ptf.getHoldingQty("US", "COP").toPlainString());
        }
        {
            LocalDate tranDate1 = parse("2024-09-03");
            FinTransaction tran1 = ptfActivity.transactions().stream().filter(t -> t.date().isEqual(tranDate1)).findAny().orElseThrow();
            assertEquals(FinTransactionType.BUY, tran1.type());
            assertEquals("AVGO", tran1.asset().symbol());
            assertEquals("0.3", tran1.qty().toPlainString());
            assertEquals("157.80", tran1.price().toPlainString());
            assertEquals("-47.34", tran1.netValue().toPlainString());
            assertEquals(0, tran1.fee().compareTo(ZERO));
        }
        {
            LocalDate tranDate1 = parse("2024-09-04");
            List<FinTransaction> multiDividTrans = ptfActivity.transactions()
                    .stream()
                    .filter(t -> t.date().isEqual(tranDate1))
                    .filter(t -> t.asset().symbol().equals("COP"))
                    .toList();
            assertEquals(2, multiDividTrans.size());
            {
                FinTransaction multiDividTran1 = multiDividTrans.get(0);
                assertEquals(FinTransactionType.DIVIDEND, multiDividTran1.type());
                assertEquals(0, multiDividTran1.qty().compareTo(ZERO));
                assertEquals("3.57", multiDividTran1.netValue().toPlainString());
                assertEquals("4.20", multiDividTran1.grossValue().toPlainString());
                assertEquals("-0.63", multiDividTran1.tax().toPlainString());
                assertEquals(0, multiDividTran1.fee().compareTo(ZERO));
            }
            {
                FinTransaction multiDividTran2 = multiDividTrans.get(1);
                assertEquals(FinTransactionType.DIVIDEND, multiDividTran2.type());
                assertEquals(0, multiDividTran2.qty().compareTo(ZERO));
                assertEquals("12.18", multiDividTran2.netValue().toPlainString());
                assertEquals("12.18", multiDividTran2.grossValue().toPlainString());
                assertEquals(0, multiDividTran2.tax().compareTo(ZERO));
                assertEquals(0, multiDividTran2.fee().compareTo(ZERO));
            }
        }
        {
            LocalDate tranDate1 = parse("2024-09-06");
            FinTransaction tran1 = ptfActivity.transactions().stream().filter(t -> t.date().isEqual(tranDate1)).findAny().orElseThrow();
            assertEquals(FinTransactionType.DIVIDEND, tran1.type());
            assertEquals("MCHP", tran1.asset().symbol());
            assertEquals(0, tran1.qty().compareTo(ZERO));
            assertEquals("19.10", tran1.netValue().toPlainString());
            assertEquals("22.47", tran1.grossValue().toPlainString());
            assertEquals("-3.37", tran1.tax().toPlainString());
            assertEquals(0, tran1.fee().compareTo(ZERO));
        }
    }

    @EnabledIf("account1IsNotNull")
    @Test
    void ptfProgress2() {
        TestContext ptfactivity = testCtx.withDmsWorkspace("rvlt-dms-stable-20240930");
        RvltModule rvltModule = ptfactivity.get(RvltModule.class);
        ValidatorFacade validator = ptfactivity.validator();
        RvltPtfActivityProvider rvltPtfProgressProvider = rvltModule.ptfProgressProvider();
        PtfActivity ptfActivity = rvltPtfProgressProvider.getPtfProgress(account1, parse("2024-10-01"), parse("2024-10-28"));
        validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
        List<DateAmount> navs = List.copyOf(ptfActivity.netAssetValues());
        assertEquals(0, navs.size());
        assertEquals(0, ptfActivity.transactions().size());
    }

    @EnabledIf("account1IsNotNull")
    @Test
    void ptfProgress3() {
        TestContext testCtx = this.testCtx.withDmsWorkspace("rvlt-dms-stable-20240930");
        RvltModule rvltModule = testCtx.get(RvltModule.class);
        ValidatorFacade validator = testCtx.validator();
        RvltPtfActivityProvider rvltPtfProgressProvider = rvltModule.ptfProgressProvider();
        PtfActivity ptfActivity = rvltPtfProgressProvider.getPtfProgress(account1, parse("2024-10-01"), parse("2024-10-01"));
        validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
        List<DateAmount> navs = List.copyOf(ptfActivity.netAssetValues());
        assertEquals(0, navs.size());
        assertEquals(0, ptfActivity.transactions().size());
    }

    @EnabledIf("account1IsNotNull")
    @Test
    void ptfProgress4() {
        TestContext testCtx = this.testCtx.withDmsWorkspace("rvlt-dms-stable-20240930");
        RvltModule rvltModule = testCtx.get(RvltModule.class);
        ValidatorFacade validator = testCtx.validator();
        RvltPtfActivityProvider rvltPtfProgressProvider = rvltModule.ptfProgressProvider();
        PtfActivity ptfActivity = rvltPtfProgressProvider.getPtfProgress(account1, parse("2024-09-30"), parse("2024-10-01"));
        validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
        List<DateAmount> navs = List.copyOf(ptfActivity.netAssetValues());
        int navsSize = navs.size();
        assertEquals(1, navsSize);
        assertEquals(new DateAmount("2024-09-30", "674.40"), navs.getFirst().with(ObfuscationUtil.remainder1000));
    }

}

