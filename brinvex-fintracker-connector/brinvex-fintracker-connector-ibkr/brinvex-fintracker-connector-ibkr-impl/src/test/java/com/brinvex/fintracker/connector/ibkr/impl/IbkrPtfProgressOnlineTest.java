package com.brinvex.fintracker.connector.ibkr.impl;


import com.brinvex.fintracker.connector.ibkr.api.IbkrModule;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrPtfProgressProvider;
import com.brinvex.fintracker.core.api.model.domain.PtfProgress;
import com.brinvex.fintracker.test.support.SimplePtf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.math.BigDecimal;

import static com.brinvex.fintracker.test.support.Country.DE;
import static com.brinvex.fintracker.test.support.Country.US;
import static java.math.RoundingMode.HALF_UP;
import static java.time.Duration.ofMinutes;
import static java.time.LocalDate.now;
import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IbkrPtfProgressOnlineTest extends BaseIbkrTest {


    // When running from IDEA, @EnableIf is ignored if @EnabledIfSystemProperty is present, but it doesn't cause problem for us.
    @EnabledIf("account2CredentialsIsNotNull")
    @EnabledIfSystemProperty(named = "enableLongRunningTests", matches = "true")
    @Test
    void portfolioProgress() {
        IbkrModule ibkrModule = newIbkrModule("dms-online1");
        IbkrPtfProgressProvider ptfProgressProvider = ibkrModule.ptfProgressProvider();

        SimplePtf ptf;
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgress(
                    account2, parse("2023-01-23"), parse("2024-06-10"), ofMinutes(0)
            );
            ptf = new SimplePtf(ptfProgress.transactions());

            assertEquals(2, ptf.getCurrencies().size());
            assertEquals(0, ptf.getHoldingQty(US, "NVDA").compareTo(new BigDecimal("60")));
        }

        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgress(
                    account2, parse("2023-01-23"), now(), ofMinutes(1)
            );

            assertTrue(ptf.getTransactions().size() <= ptfProgress.transactions().size());

            ptf = new SimplePtf(ptfProgress.transactions());
        }
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgress(
                    account2, parse("2023-01-23"), now(), ofMinutes(1)
            );
            assertTrue(ptf.getTransactions().size() <= ptfProgress.transactions().size());

            ptf = new SimplePtf(ptfProgress.transactions());
            assertTrue(ptf.getCurrencies().size() >= 2);
        }
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgress(
                    account2, parse("2023-01-23"), parse("2024-09-05"), ofMinutes(1)
            );

            ptf = new SimplePtf(ptfProgress.transactions());
            assertEquals(2, ptf.getCurrencies().size());
            assertEquals("6", ptf.getHoldingQty(US, "ILMN").toString());
        }
        {
            PtfProgress ptfProgress = ptfProgressProvider.getPortfolioProgress(
                    account2, parse("2023-01-23"), parse("2024-09-11"), ofMinutes(1)
            );
            ptf = new SimplePtf(ptfProgress.transactions());
            assertEquals(2, ptf.getCurrencies().size());
            assertEquals("59.64", ptf.getCash("EUR").setScale(2, HALF_UP).toString());
            assertEquals("517.29", ptf.getCash("USD").setScale(2, HALF_UP).toString());
            assertEquals("35", ptf.getHoldingQty(DE, "CSPX").toString());
            assertEquals("5108.86", ptfProgress.netAssetValues().getLast().amount().remainder(new BigDecimal("10000")).setScale(2, HALF_UP).toString());
        }
    }

}

