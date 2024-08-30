package com.brinvex.fintracker.connector.ibkr.impl;


import com.brinvex.fintracker.api.model.domain.FinTransaction;
import com.brinvex.fintracker.test.support.TestSupport;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.CashTransaction;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.CorporateAction;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.FlexStatement.ActivityStatement;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.Trade;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrDms;
import com.brinvex.fintracker.connector.ibkr.api.IbkrModule;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrFinTransactionMapper;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrStatementMerger;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrStatementParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IbkrMapperTest {

    private static final Logger LOG = LoggerFactory.getLogger(IbkrMapperTest.class);

    private static final TestSupport testSupport = new TestSupport("connector-ibkr");

    private static final String ibkrTestAccount1 = testSupport.property("ibkrTestAccount1.accountId");

    private static boolean ibkrTestAccount1() {
        return ibkrTestAccount1 != null;
    }

    @EnabledIf("ibkrTestAccount1")
    @Test
    void transactionMapping() {
        IbkrModule ibkrFactory = testSupport.finTracker(Map.of(IbkrModule.PROP_DMS_WORKSPACE, "dms-pers1")).get(IbkrModule.class);
        IbkrStatementParser parser = ibkrFactory.statementParser();
        IbkrStatementMerger merger = ibkrFactory.statementMerger();
        IbkrFinTransactionMapper finTranMapper = ibkrFactory.finTransactionMapper();
        IbkrDms dms = ibkrFactory.dms();
        List<ActivityStatement> actStatements = dms.getActivityDocKeys(ibkrTestAccount1, null, null)
                .stream()
                .map(dms::getStatementContent)
                .map(parser::parseActivityStatement)
                .toList();
        if (!actStatements.isEmpty()) {
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

}

