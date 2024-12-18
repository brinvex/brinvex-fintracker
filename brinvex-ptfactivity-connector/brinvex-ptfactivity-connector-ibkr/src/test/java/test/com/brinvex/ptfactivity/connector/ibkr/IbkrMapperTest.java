package test.com.brinvex.ptfactivity.connector.ibkr;


import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CashTransaction;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CorporateAction;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.FlexStatement.ActivityStatement;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.Trade;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrDms;
import com.brinvex.ptfactivity.connector.ibkr.api.IbkrModule;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrFinTransactionMapper;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrStatementMerger;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrStatementParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IbkrMapperTest extends IbkrBaseTest {

    @EnabledIf("account2IsNotNull")
    @Test
    void transactionMapping() {
        IbkrModule ibkrModule = testCtx.withDmsWorkspace("ibkr-dms-stable").get(IbkrModule.class);
        IbkrStatementParser parser = ibkrModule.statementParser();
        IbkrStatementMerger merger = ibkrModule.statementMerger();
        IbkrFinTransactionMapper finTranMapper = ibkrModule.finTransactionMapper();
        IbkrDms dms = ibkrModule.dms();
        assert account2 != null;
        List<ActivityStatement> actStatements = dms.getActivityDocKeys(account2.externalId(), null, null)
                .stream()
                .map(dms::getStatementContent)
                .map(parser::parseActivityStatement)
                .toList();
        assertFalse(actStatements.isEmpty());
        ActivityStatement mergedActStatement = merger.mergeActivityStatements(actStatements).orElseThrow();
        assertNotNull(mergedActStatement);
        {
            List<CashTransaction> rawCashTrans = mergedActStatement.cashTransactions();
            Collection<FinTransaction> trans = finTranMapper.mapCashTransactions(rawCashTrans);
            assertNotNull(trans);
            if (!rawCashTrans.isEmpty()) {
                assertFalse(trans.isEmpty());
                assertTrue(trans.size() <= rawCashTrans.size());
            } else {
                assertTrue(trans.isEmpty());
            }
        }
        {
            List<Trade> rawTrades = mergedActStatement.trades();
            Collection<FinTransaction> trans = finTranMapper.mapTrades(rawTrades);
            assertNotNull(trans);
            assertEquals(rawTrades.size(), trans.size());
        }
        {
            List<CorporateAction> rawCorpActions = mergedActStatement.corporateActions();
            Collection<FinTransaction> trans = finTranMapper.mapCorporateAction(rawCorpActions);
            assertNotNull(trans);
            assertEquals(rawCorpActions.size(), trans.size());
        }
    }

}

