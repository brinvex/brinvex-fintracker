package com.brinvex.fintracker.connector.ibkr.impl;


import com.brinvex.fintracker.connector.ibkr.api.model.IbkrAccount;
import com.brinvex.fintracker.connector.ibkr.api.IbkrModule;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.FlexStatement.ActivityStatement;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.FlexStatement.TradeConfirmStatement;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrFetcher;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrStatementParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IbkrFetcherTest extends BaseIbkrTest {


    // When running from IDEA, @EnableIf is ignored if @EnabledIfSystemProperty is present, but it doesn't cause problem for us.
    @EnabledIf("account2CredentialsIsNotNull")
    @EnabledIfSystemProperty(named = "enableLongRunningTests", matches = "true")
    @Test
    void fetch() throws InterruptedException {
        IbkrModule ibkrModule = moduleTestSupport.finTracker().module(IbkrModule.class);
        IbkrFetcher fetcher = ibkrModule.fetcher();
        IbkrStatementParser parser = ibkrModule.statementParser();

        IbkrAccount.Credentials credentials = account2.credentials();
        {
            String content = fetcher.fetchFlexStatement(credentials.token(), credentials.activityFlexQueryId(), 1, ofSeconds(6));
            ActivityStatement actStatement = parser.parseActivityStatement(content);
            assertEquals(account2.accountId(), actStatement.accountId());
        }
        Thread.sleep(ofSeconds(1));
        {
            String content = fetcher.fetchFlexStatement(credentials.token(), credentials.activityFlexQueryId(), 4, ofSeconds(1));
            ActivityStatement actStatement = parser.parseActivityStatement(content);
            assertEquals(account2.accountId(), actStatement.accountId());
        }
        Thread.sleep(ofSeconds(1));
        {
            String content = fetcher.fetchFlexStatement(credentials.token(), credentials.tradeConfirmFlexQueryId(), 1, ofSeconds(0));
            TradeConfirmStatement tcStatement = parser.parseTradeConfirmStatement(content);
            assertEquals(account2.accountId(), tcStatement.accountId());
        }
    }

}

