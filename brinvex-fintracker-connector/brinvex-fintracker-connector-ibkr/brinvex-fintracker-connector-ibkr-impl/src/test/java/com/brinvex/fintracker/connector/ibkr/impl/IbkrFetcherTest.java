package com.brinvex.fintracker.connector.ibkr.impl;


import com.brinvex.fintracker.common.test.TestSupport;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.FlexStatement.ActivityStatement;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.FlexStatement.TradeConfirmStatement;
import com.brinvex.fintracker.connector.ibkr.api.factory.IbkrFactory;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrFetcher;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrStatementParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IbkrFetcherTest {

    private static final Logger LOG = LoggerFactory.getLogger(IbkrFetcherTest.class);

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
    void fetch() throws InterruptedException {
        IbkrFetcher fetcher = ibkrFactory.fetcher(testSupport.httpClientFacade());
        IbkrStatementParser parser = ibkrFactory.statementParser();

        {
            String content = fetcher.fetchFlexStatement(ibkrTestAccount1Token, ibkrTestAccount1ActFlexQueryId, 1, ofSeconds(6));
            ActivityStatement actStatement = parser.parseActivityStatement(content);
            assertEquals(ibkrTestAccount1, actStatement.accountId());
        }
        Thread.sleep(ofSeconds(1));
        {
            String content = fetcher.fetchFlexStatement(ibkrTestAccount1Token, ibkrTestAccount1ActFlexQueryId, 4, ofSeconds(1));
            ActivityStatement actStatement = parser.parseActivityStatement(content);
            assertEquals(ibkrTestAccount1, actStatement.accountId());
        }
        Thread.sleep(ofSeconds(1));
        {
            String content = fetcher.fetchFlexStatement(ibkrTestAccount1Token, ibkrTestAccount1TradeConfirmFlexQueryId, 1, ofSeconds(0));
            TradeConfirmStatement tcStatement = parser.parseTradeConfirmStatement(content);
            assertEquals(ibkrTestAccount1, tcStatement.accountId());
        }
    }

}

