package com.brinvex.fintracker.connector.ibkr.impl;


import com.brinvex.fintracker.api.model.domain.FinTransaction;
import com.brinvex.fintracker.api.model.domain.FinTransactionType;
import com.brinvex.fintracker.api.model.domain.PtfProgress;
import com.brinvex.fintracker.test.support.SimplePtf;
import com.brinvex.fintracker.test.support.TestSupport;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrDocKey.ActivityDocKey;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrDms;
import com.brinvex.fintracker.connector.ibkr.api.IbkrModule;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrPtfProgressProvider;
import com.brinvex.fintracker.connector.ibkr.impl.service.IbkrDmsImpl;
import com.brinvex.util.dms.api.Dms;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.brinvex.fintracker.test.support.Country.US;
import static com.brinvex.fintracker.test.support.Currency.EUR;
import static com.brinvex.fintracker.test.support.Currency.USD;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.time.LocalDate.now;
import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class IbkrPtfProgressOfflineTest {

    private static final Logger LOG = LoggerFactory.getLogger(IbkrPtfProgressOfflineTest.class);

    private static final TestSupport testSupport = new TestSupport("connector-ibkr");

    private static final String ibkrTestAccount1 = testSupport.property("ibkrTestAccount1.accountId");

    private static boolean ibkrTestAccount1() {
        return ibkrTestAccount1 != null;
    }

    @EnabledIf("ibkrTestAccount1")
    @Test
    void portfolioProgress_iterative() {
        String workspace = "dms-pers1";
        IbkrModule ibkrFactory = testSupport.app(Map.of(IbkrModule.PROP_DMS_WORKSPACE, workspace)).get(IbkrModule.class);
        Dms dms = testSupport.dmsFactory().getDms(workspace);
        IbkrDms ibkrDms = new IbkrDmsImpl(dms);
        IbkrPtfProgressProvider ptfProgressProvider = ibkrFactory.ptfProgressProvider();
        LocalDate today = now();

        List<ActivityDocKey> docKeys = ibkrDms.getActivityDocKeys(ibkrTestAccount1, LocalDate.MIN, today);
        assertFalse(docKeys.isEmpty());
        for (LocalDate d = docKeys.getFirst().fromDateIncl(); d.isBefore(now()); d = d.plusMonths(1)) {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(ibkrTestAccount1, d, today);
            assertNotNull(ptfProgress);
        }
    }

    @EnabledIf("ibkrTestAccount1")
    @Test
    void portfolioProgress_spinOff() {
        String workspace = "dms-pers1";
        IbkrModule ibkrFactory = testSupport.app(Map.of(IbkrModule.PROP_DMS_WORKSPACE, workspace)).get(IbkrModule.class);
        IbkrPtfProgressProvider ptfProgressProvider = ibkrFactory.ptfProgressProvider();

        PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(ibkrTestAccount1, parse("2022-08-03"), parse("2024-04-02"));
        SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

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

    @Test
    void portfolioProgress_paymentOfLieuOfDividends() {
        String workspace = "dms-pers1";
        IbkrModule ibkrFactory = testSupport.app(Map.of(IbkrModule.PROP_DMS_WORKSPACE, workspace)).get(IbkrModule.class);
        IbkrPtfProgressProvider ptfProgressProvider = ibkrFactory.ptfProgressProvider();

        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(ibkrTestAccount1, parse("2022-08-03"), parse("2024-06-28"));
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(314, ptf.getTransactions().size());
            FinTransaction tran = ptf.getTransactions().get(310);
            assertEquals(tran.type(), FinTransactionType.DIVIDEND);
            assertEquals(tran.extraType(), "PAYMENT_IN_LIEU_OF_DIVIDENDS");
            assertEquals(tran.asset().symbol(), "ARCC");
        }
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(ibkrTestAccount1, parse("2022-08-03"), parse("2024-07-15"));
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(326, ptf.getTransactions().size());
            FinTransaction tran = ptf.getTransactions().get(310);
            assertEquals(tran.type(), FinTransactionType.DIVIDEND);
            assertEquals(tran.extraType(), "PAYMENT_IN_LIEU_OF_DIVIDENDS");
            assertEquals(tran.asset().symbol(), "ARCC");
        }
    }

    @Test
    void portfolioProgress_tradeConfirm() {

        List<FinTransaction> actAndTcTrans;

        {
            IbkrModule ibkrFactory = testSupport.app(Map.of(IbkrModule.PROP_DMS_WORKSPACE, "dms-pers2")).get(IbkrModule.class);
            IbkrPtfProgressProvider ptfProgressProvider = ibkrFactory.ptfProgressProvider();

            List<FinTransaction> actTrans1;
            {
                PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(ibkrTestAccount1, parse("2022-08-03"), parse("2024-04-16"));
                SimplePtf ptf = new SimplePtf(ptfProgress.transactions());
                actTrans1 = ptf.getTransactions();
                assertEquals(243, actTrans1.size());
                assertEquals(0, ptf.getHoldingQty(US, "MU").compareTo(new BigDecimal("1")));
            }
            {
                PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(ibkrTestAccount1, parse("2022-08-03"), parse("2024-04-17"));
                SimplePtf ptf = new SimplePtf(ptfProgress.transactions());
                assertEquals(actTrans1, ptf.getTransactions());
            }
            {
                PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(ibkrTestAccount1, parse("2022-08-03"), parse("2024-04-18"));
                SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

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
            IbkrModule ibkrFactory = testSupport.app(Map.of(IbkrModule.PROP_DMS_WORKSPACE, "dms-pers1")).get(IbkrModule.class);
            IbkrPtfProgressProvider ptfProgressProvider = ibkrFactory.ptfProgressProvider();

            {
                PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(ibkrTestAccount1, parse("2022-08-03"), parse("2024-04-18"));
                SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

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

    @Test
    void ptfProgress_corpActions() {
        IbkrModule ibkrFactory = testSupport.app(Map.of(IbkrModule.PROP_DMS_WORKSPACE, "dms-pers1")).get(IbkrModule.class);
        IbkrPtfProgressProvider ptfProgressProvider = ibkrFactory.ptfProgressProvider();

        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(ibkrTestAccount1, parse("2022-08-03"), parse("2023-08-02"));
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getCash(EUR).compareTo(new BigDecimal("43.659223735")));
            assertEquals(0, ptf.getCash(USD).compareTo(new BigDecimal("0.402378700")));

            assertEquals(15, ptf.getHoldingsCount());
        }
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(ibkrTestAccount1, parse("2022-08-03"), parse("2023-11-17"));
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getCash(EUR).compareTo(new BigDecimal("722.811854405")));
            assertEquals(0, ptf.getCash(USD).compareTo(new BigDecimal("183.601774170")));

            assertEquals(23, ptf.getHoldingsCount());
        }
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(ibkrTestAccount1, parse("2022-08-03"), parse("2023-11-29"));
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getCash(EUR).compareTo(new BigDecimal("722.811854405")));
            assertEquals(0, ptf.getCash(USD).compareTo(new BigDecimal("1071.101774170")));

            assertEquals(23, ptf.getHoldingsCount());
            assertEquals(0, ptf.getHoldingQty(US, "VMW").compareTo(ZERO));
        }
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(ibkrTestAccount1, parse("2022-08-03"), parse("2024-04-30"));
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getCash(EUR).setScale(2, HALF_UP).compareTo(new BigDecimal("284.92")));
            assertEquals(0, ptf.getCash(USD).setScale(2, HALF_UP).compareTo(new BigDecimal("164.64")));
        }
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(ibkrTestAccount1, parse("2022-08-03"), parse("2024-05-31"));
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getCash(EUR).setScale(2, HALF_UP).compareTo(new BigDecimal("130.13")));
            assertEquals(0, ptf.getCash(USD).setScale(2, HALF_UP).compareTo(new BigDecimal("54.07")));
        }
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(ibkrTestAccount1, parse("2022-08-03"), parse("2024-06-05"));
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getCash(EUR).setScale(2, HALF_UP).compareTo(new BigDecimal("1090.13")));
            assertEquals(0, ptf.getCash(USD).setScale(2, HALF_UP).compareTo(new BigDecimal("64.89")));
        }
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(ibkrTestAccount1, parse("2022-08-03"), parse("2024-06-10"));
            SimplePtf ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getHoldingQty(US, "NVDA").compareTo(new BigDecimal("60")));
        }
    }

}

