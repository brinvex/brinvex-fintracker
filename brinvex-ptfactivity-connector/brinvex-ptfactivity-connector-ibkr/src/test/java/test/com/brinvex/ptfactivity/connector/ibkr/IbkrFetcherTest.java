package test.com.brinvex.ptfactivity.connector.ibkr;


import com.brinvex.ptfactivity.connector.ibkr.api.model.IbkrAccount;
import com.brinvex.ptfactivity.connector.ibkr.api.IbkrModule;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.FlexStatement.ActivityStatement;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.FlexStatement.TradeConfirmStatement;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrFetcher;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrStatementParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IbkrFetcherTest extends IbkrBaseTest {


    // When running from IDEA, @EnableIf is ignored if @EnabledIfSystemProperty is present, but it doesn't cause problem for us.
    @EnabledIf("account2CredentialsIsNotNull")
    @EnabledIfSystemProperty(named = "enableConfidentialTests", matches = "true")
    @Test
    void fetch() throws InterruptedException {
        assert account2 != null;

        IbkrModule ibkrModule = testCtx.get(IbkrModule.class);
        IbkrFetcher fetcher = ibkrModule.fetcher();
        IbkrStatementParser parser = ibkrModule.statementParser();

        IbkrAccount.Credentials credentials = account2.credentials();
        {
            String content = fetcher.fetchFlexStatement(credentials.token(), credentials.activityFlexQueryId(), 1, ofSeconds(6));
            ActivityStatement actStatement = parser.parseActivityStatement(content);
            assertEquals(account2.externalId(), actStatement.accountId());
        }
        Thread.sleep(ofSeconds(1));
        {
            String content = fetcher.fetchFlexStatement(credentials.token(), credentials.activityFlexQueryId(), 4, ofSeconds(1));
            ActivityStatement actStatement = parser.parseActivityStatement(content);
            assertEquals(account2.externalId(), actStatement.accountId());
        }
        Thread.sleep(ofSeconds(1));
        {
            String content = fetcher.fetchFlexStatement(credentials.token(), credentials.tradeConfirmFlexQueryId(), 1, ofSeconds(0));
            TradeConfirmStatement tcStatement = parser.parseTradeConfirmStatement(content);
            assertEquals(account2.externalId(), tcStatement.accountId());
        }
    }

}

