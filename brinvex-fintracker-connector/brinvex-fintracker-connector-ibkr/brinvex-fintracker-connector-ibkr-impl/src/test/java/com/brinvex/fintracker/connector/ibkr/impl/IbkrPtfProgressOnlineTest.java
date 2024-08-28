package com.brinvex.fintracker.connector.ibkr.impl;


import com.brinvex.fintracker.api.model.domain.PtfProgress;
import com.brinvex.fintracker.common.test.SimplePtf;
import com.brinvex.fintracker.common.test.TestSupport;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrCredentials;
import com.brinvex.fintracker.connector.ibkr.api.factory.IbkrFactory;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrPtfProgressProvider;
import com.brinvex.util.dms.api.Dms;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import static com.brinvex.fintracker.common.test.Country.US;
import static java.time.Duration.ofMinutes;
import static java.time.LocalDate.now;
import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IbkrPtfProgressOnlineTest {

    private static final Logger LOG = LoggerFactory.getLogger(IbkrPtfProgressOnlineTest.class);

    private static final TestSupport testSupport = new TestSupport("connector-ibkr");

    private static final String ibkrTestAccount1 = testSupport.property("ibkrTestAccount1.accountId");

    private static final String ibkrTestAccount1Token = testSupport.property("ibkrTestAccount1.token");

    private static final String ibkrTestAccount1ActFlexQueryId = testSupport.property("ibkrTestAccount1.activityFlexQueryId");

    private static final String ibkrTestAccount1TradeConfirmFlexQueryId = testSupport.property("ibkrTestAccount1.tradeConfirmationFlexQueryId");

    private static final IbkrFactory ibkrFactory = IbkrFactory.create();

    private static boolean ibkrTestAccount1Credentials() {
        return ibkrTestAccount1 != null
               && ibkrTestAccount1Token != null
               && ibkrTestAccount1ActFlexQueryId != null
               && ibkrTestAccount1TradeConfirmFlexQueryId != null;
    }

    // When running from IDEA, @EnableIf is ignored if @EnabledIfSystemProperty is present, but it doesn't cause problem for us.
    @EnabledIf("ibkrTestAccount1Credentials")
    @EnabledIfSystemProperty(named = "enableLongRunningTests", matches = "true")
    @Test
    void portfolioProgress() {
        String workspace = "dms-online1";
        Dms dms = testSupport.dmsFactory().getDms(workspace);
        IbkrPtfProgressProvider ptfProgressProvider = ibkrFactory.ptfProgressProvider(dms, testSupport.httpClientFacade());

        IbkrCredentials ibkrCredentials = new IbkrCredentials(
                ibkrTestAccount1Token,
                ibkrTestAccount1ActFlexQueryId,
                ibkrTestAccount1TradeConfirmFlexQueryId
        );


        SimplePtf ptf;
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOnline(ibkrTestAccount1, ibkrCredentials, parse("2022-08-03"), parse("2024-06-10"), ofMinutes(0));
            ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getHoldingQty(US, "NVDA").compareTo(new BigDecimal("60")));
        }

        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOnline(ibkrTestAccount1, ibkrCredentials, parse("2022-08-03"), now(), ofMinutes(1));

            assertTrue(ptf.getTransactions().size() <= ptfProgress.transactions().size());

            ptf = new SimplePtf(ptfProgress.transactions());
        }
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgressOnline(ibkrTestAccount1, ibkrCredentials, parse("2022-08-03"), now(), ofMinutes(1));
            assertTrue(ptf.getTransactions().size() <= ptfProgress.transactions().size());

            ptf = new SimplePtf(ptfProgress.transactions());
            assertTrue(ptf.getCurrencies().size() >= 2);
        }
    }

}

