package test.com.brinvex.ptfactivity.connector.ibkr;


import com.brinvex.finance.types.vo.DateAmount;
import com.brinvex.ptfactivity.connector.ibkr.api.IbkrModule;
import com.brinvex.ptfactivity.connector.ibkr.api.model.IbkrDocKey.ActivityDocKey;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrDms;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrPtfActivityProvider;
import com.brinvex.ptfactivity.core.api.domain.PtfActivity;
import com.brinvex.ptfactivity.core.api.facade.ValidatorFacade;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType;
import com.brinvex.ptfactivity.core.api.domain.constraints.fintransaction.FinTransactionConstraints;
import com.brinvex.ptfactivity.testsupport.TestContext;
import com.brinvex.ptfactivity.testsupport.SimplePtf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static com.brinvex.finance.types.enu.Currency.EUR;
import static com.brinvex.finance.types.enu.Currency.USD;
import static com.brinvex.ptfactivity.testsupport.Country.US;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.time.LocalDate.now;
import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class IbkrPtfActivityOfflineTest extends IbkrBaseTest {

    @EnabledIf("account1IsNotNull")
    @Test
    void portfolioProgress_iterative() {
        String workspace = "ibkr-dms-stable";
        TestContext testCtx = this.testCtx.withDmsWorkspace(workspace);
        IbkrModule ibkrModule = testCtx.get(IbkrModule.class);
        IbkrDms ibkrDms = ibkrModule.dms();
        IbkrPtfActivityProvider ptfProgressProvider = ibkrModule.ptfProgressProvider();
        ValidatorFacade validator = testCtx.validator();
        LocalDate today = now();

        assert account1 != null;
        List<ActivityDocKey> docKeys = ibkrDms.getActivityDocKeys(account1.externalId(), LocalDate.MIN, today);
        assertFalse(docKeys.isEmpty());
        for (LocalDate d = docKeys.getFirst().fromDateIncl(); d.isBefore(docKeys.getLast().toDateIncl()); d = d.plusMonths(1)) {
            PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(account2, d, today);
            assertNotNull(ptfActivity, "d=%s".formatted(d));
            validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
        }
    }

    @EnabledIf("account2IsNotNull")
    @Test
    void portfolioProgress_spinOff() {
        String workspace = "ibkr-dms-stable";
        TestContext testCtx = this.testCtx.withDmsWorkspace(workspace);
        IbkrModule ibkrModule = testCtx.get(IbkrModule.class);
        IbkrPtfActivityProvider ptfProgressProvider = ibkrModule.ptfProgressProvider();
        ValidatorFacade validator = testCtx.validator();

        PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(
                account2, parse("2023-01-23"), parse("2024-04-02"));
        validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);

        SimplePtf ptf = new SimplePtf(ptfActivity.transactions());

        assertEquals(0, ptf.getHoldingQty(US, "GE").compareTo(new BigDecimal(6)));
        assertEquals(0, ptf.getHoldingQty(US, "GEV").compareTo(new BigDecimal(1)));
        assertEquals(0, ptf.getCash(EUR).compareTo(new BigDecimal("234.561374405")));
        assertEquals(0, ptf.getCash(USD).compareTo(new BigDecimal("153.48807417")));

        FinTransaction transformationTran = ptf.getTransactions().get(225);
        assertEquals(FinTransactionType.TRANSFORMATION, transformationTran.type());
        assertEquals("GEV", transformationTran.asset().symbol());
        FinTransaction sellTran = ptf.getTransactions().get(226);
        assertEquals(FinTransactionType.SELL, sellTran.type());
        assertEquals("GEV", sellTran.asset().symbol());
    }

    @EnabledIf("account2IsNotNull")
    @Test
    void portfolioProgress_paymentOfLieuOfDividends() {
        String workspace = "ibkr-dms-stable";
        TestContext testCtx = this.testCtx.withDmsWorkspace(workspace);
        IbkrModule ibkrModule = testCtx.get(IbkrModule.class);
        IbkrPtfActivityProvider ptfProgressProvider = ibkrModule.ptfProgressProvider();
        ValidatorFacade validator = testCtx.validator();

        {
            PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(
                    account2, parse("2023-01-23"), parse("2024-06-28"));
            validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
            SimplePtf ptf = new SimplePtf(ptfActivity.transactions());

            assertEquals(314, ptf.getTransactions().size());
            FinTransaction tran = ptf.getTransactions().get(310);
            assertEquals(FinTransactionType.DIVIDEND, tran.type());
            assertEquals("PAYMENT_IN_LIEU_OF_DIVIDENDS", tran.externalType());
            assertEquals("ARCC", tran.asset().symbol());
        }
        {
            PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(
                    account2, parse("2023-01-23"), parse("2024-07-15"));
            validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
            SimplePtf ptf = new SimplePtf(ptfActivity.transactions());

            assertEquals(326, ptf.getTransactions().size());
            FinTransaction tran = ptf.getTransactions().get(310);
            assertEquals(tran.type(), FinTransactionType.DIVIDEND);
            assertEquals(tran.externalType(), "PAYMENT_IN_LIEU_OF_DIVIDENDS");
            assertEquals(tran.asset().symbol(), "ARCC");
        }
    }

    @EnabledIf("account2IsNotNull")
    @Test
    void portfolioProgress_tradeConfirm() {

        List<FinTransaction> actAndTcTrans;

        {
            TestContext testCtx = this.testCtx.withDmsWorkspace("ibkr-dms-stable-20240418");
            IbkrModule ibkrModule = testCtx.get(IbkrModule.class);
            IbkrPtfActivityProvider ptfProgressProvider = ibkrModule.ptfProgressProvider();
            ValidatorFacade validator = testCtx.validator();

            List<FinTransaction> actTrans1;
            {
                PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(
                        account2, parse("2023-01-23"), parse("2024-04-16"));
                validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
                SimplePtf ptf = new SimplePtf(ptfActivity.transactions());
                actTrans1 = ptf.getTransactions();
                assertEquals(243, actTrans1.size());
                assertEquals(0, ptf.getHoldingQty(US, "MU").compareTo(new BigDecimal("1")));
            }
            {
                PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(
                        account2, parse("2023-01-23"), parse("2024-04-17"));
                validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
                SimplePtf ptf = new SimplePtf(ptfActivity.transactions());
                assertEquals(actTrans1, ptf.getTransactions());
            }
            {
                PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(
                        account2, parse("2023-01-23"), parse("2024-04-18"));
                validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
                SimplePtf ptf = new SimplePtf(ptfActivity.transactions());

                actAndTcTrans = ptf.getTransactions();
                assertEquals(actTrans1.size() + 2, actAndTcTrans.size());

                FinTransaction newestTran = actAndTcTrans.getLast();
                assertEquals(FinTransactionType.BUY, newestTran.type());
                assertEquals(parse("2024-04-18"), newestTran.date());
                assertEquals("MU", newestTran.asset().symbol());
                assertEquals(0, ptf.getHoldingQty(US, "MU").compareTo(new BigDecimal("6")));
            }
        }
        {
            TestContext testCtx = this.testCtx.withDmsWorkspace("ibkr-dms-stable");
            IbkrModule ibkrModule = testCtx.get(IbkrModule.class);
            IbkrPtfActivityProvider ptfProgressProvider = ibkrModule.ptfProgressProvider();
            ValidatorFacade validator = testCtx.validator();

            {
                PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(
                        account2, parse("2023-01-23"), parse("2024-04-18"));
                validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
                SimplePtf ptf = new SimplePtf(ptfActivity.transactions());

                List<FinTransaction> actTrans2 = ptf.getTransactions();
                assertTrue(actAndTcTrans.size() <= actTrans2.size());

                Set<FinTransactionType> tcTranTypes = Set.of(FinTransactionType.BUY, FinTransactionType.SELL, FinTransactionType.FX_BUY, FinTransactionType.FX_SELL);
                for (int i = 0, j = 0; j < actTrans2.size(); i++, j++) {
                    FinTransaction actAndTcTran = actAndTcTrans.get(i);
                    FinTransaction actTran2 = actTrans2.get(i);
                    if (actAndTcTran.equals(actTran2)) {
                        continue;
                    }
                    if (actTran2.date().isEqual(parse("2024-04-18"))) {
                        if (!tcTranTypes.contains(actTran2.type())) {
                            i--;
                            continue;
                        }
                    }
                    fail("actAndTcTran=%s, actTran2=%s".formatted(actAndTcTran, actTran2));
                }
            }
        }
    }

    @EnabledIf("account2IsMigrated")
    @Test
    void ptfProgress_accountMigration_newNavSameAsOld() {
        TestContext testCtx = this.testCtx.withDmsWorkspace("ibkr-dms-stable");
        IbkrModule ibkrModule = testCtx.get(IbkrModule.class);
        IbkrPtfActivityProvider ptfProgressProvider = ibkrModule.ptfProgressProvider();
        ValidatorFacade validator = testCtx.validator();

        {
            PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(
                    account2, parse("2023-01-23"), parse("2024-09-02"));
            validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
            for (DateAmount nav : ptfActivity.netAssetValues()) {
                assertTrue(nav.amount().compareTo(ZERO) > 0, nav::toString);
            }

            SimplePtf ptf = new SimplePtf(ptfActivity.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals("49.10", ptf.getCash(EUR).setScale(2, HALF_UP).toString());
            assertEquals("105.82", ptf.getCash(USD).setScale(2, HALF_UP).toString());

            assertEquals(39, ptf.getHoldingsCount());
        }
    }

    @EnabledIf("account2IsMigrated")
    @Test
    void ptfProgress_accountMigration_navDate() {
        TestContext testCtx = this.testCtx.withDmsWorkspace("ibkr-dms-stable");
        IbkrModule ibkrModule = testCtx.get(IbkrModule.class);
        IbkrPtfActivityProvider ptfProgressProvider = ibkrModule.ptfProgressProvider();
        ValidatorFacade validator = testCtx.validator();
        PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(
                account2, parse("2023-01-23"), parse("2024-09-11"));
        validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
        assertEquals(parse("2024-09-11"), ptfActivity.netAssetValues().getLast().date());
    }

    @EnabledIf("account2IsNotNull")
    @Test
    void ptfProgress_corpActions() {
        TestContext testCtx = this.testCtx.withDmsWorkspace("ibkr-dms-stable");
        IbkrModule ibkrModule = testCtx.get(IbkrModule.class);
        IbkrPtfActivityProvider ptfProgressProvider = ibkrModule.ptfProgressProvider();
        ValidatorFacade validator = testCtx.validator();
        {
            PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(
                    account2, parse("2023-01-23"), parse("2023-08-02"));
            validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
            SimplePtf ptf = new SimplePtf(ptfActivity.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getCash(EUR).compareTo(new BigDecimal("43.659223735")));
            assertEquals(0, ptf.getCash(USD).compareTo(new BigDecimal("0.402378700")));

            assertEquals(15, ptf.getHoldingsCount());
        }
        {
            PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(
                    account2, parse("2023-01-23"), parse("2023-11-17"));
            validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
            SimplePtf ptf = new SimplePtf(ptfActivity.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getCash(EUR).compareTo(new BigDecimal("722.811854405")));
            assertEquals(0, ptf.getCash(USD).compareTo(new BigDecimal("183.601774170")));

            assertEquals(23, ptf.getHoldingsCount());
        }
        {
            PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(
                    account2, parse("2023-01-23"), parse("2023-11-29"));
            validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
            SimplePtf ptf = new SimplePtf(ptfActivity.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getCash(EUR).compareTo(new BigDecimal("722.811854405")));
            assertEquals(0, ptf.getCash(USD).compareTo(new BigDecimal("1071.101774170")));

            assertEquals(23, ptf.getHoldingsCount());
            assertEquals(0, ptf.getHoldingQty(US, "VMW").compareTo(ZERO));
        }
        {
            PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(
                    account2, parse("2023-01-23"), parse("2024-04-30"));
            validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
            SimplePtf ptf = new SimplePtf(ptfActivity.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getCash(EUR).setScale(2, HALF_UP).compareTo(new BigDecimal("284.92")));
            assertEquals(0, ptf.getCash(USD).setScale(2, HALF_UP).compareTo(new BigDecimal("164.64")));
        }
        {
            PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(
                    account2, parse("2023-01-23"), parse("2024-05-31"));
            validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
            SimplePtf ptf = new SimplePtf(ptfActivity.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getCash(EUR).setScale(2, HALF_UP).compareTo(new BigDecimal("130.13")));
            assertEquals(0, ptf.getCash(USD).setScale(2, HALF_UP).compareTo(new BigDecimal("54.07")));
        }
        {
            PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(
                    account2, parse("2023-01-23"), parse("2024-06-05"));
            validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
            SimplePtf ptf = new SimplePtf(ptfActivity.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getCash(EUR).setScale(2, HALF_UP).compareTo(new BigDecimal("1090.13")));
            assertEquals(0, ptf.getCash(USD).setScale(2, HALF_UP).compareTo(new BigDecimal("64.89")));
        }
        {
            PtfActivity ptfActivity = ptfProgressProvider.getPtfProgressOffline(
                    account2, parse("2023-01-23"), parse("2024-06-10"));
            validator.validateAndThrow(ptfActivity.transactions(), FinTransactionConstraints::of);
            SimplePtf ptf = new SimplePtf(ptfActivity.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getHoldingQty(US, "NVDA").compareTo(new BigDecimal("60")));
        }
    }

}

