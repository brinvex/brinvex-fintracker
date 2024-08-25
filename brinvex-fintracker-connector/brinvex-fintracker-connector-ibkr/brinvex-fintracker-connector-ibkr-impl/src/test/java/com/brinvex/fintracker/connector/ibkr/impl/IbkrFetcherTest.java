package com.brinvex.fintracker.connector.ibkr.impl;


import com.brinvex.fintracker.common.impl.facade.HttpClientFacadeImpl;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.FlexStatement.ActivityStatement;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.FlexStatement.TradeConfirmStatement;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrFetcher;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrStatementParser;
import com.brinvex.fintracker.connector.ibkr.impl.service.IbkrFetcherImpl;
import com.brinvex.fintracker.connector.ibkr.impl.service.IbkrStatementParserImpl;
import com.brinvex.fintracker.common.test.TestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IbkrFetcherTest {

    private static final Logger LOG = LoggerFactory.getLogger(IbkrFetcherTest.class);

    private static final TestSupport testSupport = new TestSupport("connector-ibkr");

    private static final String ibkrTestAccount1 = testSupport.property("ibkrTestAccount1.accountId");

    private static final String ibkrTestAccount1Token = testSupport.property("ibkrTestAccount1.token");

    private static final String ibkrTestAccount1ActFlexQueryId = testSupport.property("ibkrTestAccount1.activityFlexQueryId");

    private static final String ibkrTestAccount1TradeConfirmFlexQueryId = testSupport.property("ibkrTestAccount1.tradeConfirmationFlexQueryId");

    private static boolean ibkrTestAccount1CredentialsIs() {
        return ibkrTestAccount1 != null
               && ibkrTestAccount1Token != null
               && ibkrTestAccount1ActFlexQueryId != null
               && ibkrTestAccount1TradeConfirmFlexQueryId != null;
    }

    // When running from IDEA, @EnableIf is ignored if @EnabledIfSystemProperty is present
    @EnabledIf("ibkrTestAccount1CredentialsIs")
    @EnabledIfSystemProperty(named = "enableLongRunningTests", matches = "true")
    @Test
    void fetch() throws InterruptedException {
        IbkrFetcher fetcher = new IbkrFetcherImpl(new HttpClientFacadeImpl());
        IbkrStatementParser parser = new IbkrStatementParserImpl();

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

