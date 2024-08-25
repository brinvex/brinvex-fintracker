package com.brinvex.fintracker.connector.ibkr.impl;


import com.brinvex.fintracker.api.model.domain.FinTransaction;
import com.brinvex.fintracker.api.model.domain.FinTransactionType;
import com.brinvex.fintracker.api.model.domain.PtfProgress;
import com.brinvex.fintracker.common.impl.facade.HttpClientFacadeImpl;
import com.brinvex.fintracker.common.test.SimplePtf;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrDocKey.ActivityDocKey;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrDms;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrPtfProgressProvider;
import com.brinvex.fintracker.connector.ibkr.impl.service.IbkrDmsImpl;
import com.brinvex.fintracker.connector.ibkr.impl.service.IbkrFetcherImpl;
import com.brinvex.fintracker.connector.ibkr.impl.service.IbkrPtfProgressProviderImpl;
import com.brinvex.fintracker.connector.ibkr.impl.service.IbkrStatementMergerImpl;
import com.brinvex.fintracker.connector.ibkr.impl.service.IbkrStatementParserImpl;
import com.brinvex.fintracker.connector.ibkr.impl.service.IbkrTransactionMapperImpl;
import com.brinvex.fintracker.common.test.TestSupport;
import com.brinvex.util.dms.api.Dms;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.brinvex.fintracker.common.test.Country.US;
import static com.brinvex.fintracker.common.test.Currency.EUR;
import static com.brinvex.fintracker.common.test.Currency.USD;
import static java.time.LocalDate.now;
import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class IbkrPtfProgressProviderTest {

    private static final Logger LOG = LoggerFactory.getLogger(IbkrPtfProgressProviderTest.class);

    private static final TestSupport testSupport = new TestSupport("connector-ibkr");

    private static final String ibkrTestAccount1 = testSupport.property("ibkrTestAccount1.accountId");

    private static final String ibkrTestAccount1Token = testSupport.property("ibkrTestAccount1.token");

    private static final String ibkrTestAccount1ActFlexQueryId = testSupport.property("ibkrTestAccount1.activityFlexQueryId");

    private static final String ibkrTestAccount1TradeConfirmFlexQueryId = testSupport.property("ibkrTestAccount1.tradeConfirmationFlexQueryId");

    private static boolean ibkrTestAccount1() {
        return ibkrTestAccount1 != null;
    }

    private static boolean ibkrTestAccount1CredentialsIs() {
        return ibkrTestAccount1 != null
               && ibkrTestAccount1Token != null
               && ibkrTestAccount1ActFlexQueryId != null
               && ibkrTestAccount1TradeConfirmFlexQueryId != null;
    }

    @EnabledIf("ibkrTestAccount1")
    @Test
    void portfolioProgress_offline() {
        String workspace = "dms-pers1";
        Dms dms = testSupport.dmsFactory().getDms(workspace);
        IbkrDms ibkrDms = new IbkrDmsImpl(dms);
        IbkrPtfProgressProvider ptfProgressProvider = new IbkrPtfProgressProviderImpl(
                ibkrDms,
                new IbkrStatementParserImpl(),
                new IbkrFetcherImpl(new HttpClientFacadeImpl()),
                new IbkrStatementMergerImpl(),
                new IbkrTransactionMapperImpl()
        );
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
    void portfolioProgress_offline_spinOff() {
        String workspace = "dms-pers1";
        Dms dms = testSupport.dmsFactory().getDms(workspace);
        IbkrDms ibkrDms = new IbkrDmsImpl(dms);
        IbkrPtfProgressProvider ptfProgressProvider = new IbkrPtfProgressProviderImpl(
                ibkrDms,
                new IbkrStatementParserImpl(),
                new IbkrFetcherImpl(new HttpClientFacadeImpl()),
                new IbkrStatementMergerImpl(),
                new IbkrTransactionMapperImpl()
        );

        SimplePtf ptf = new SimplePtf();

        PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOffline(ibkrTestAccount1, parse("2022-08-03"), parse("2024-04-02"));
        ptf.applyTransactions(ptfProgress.transactions());

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

}

