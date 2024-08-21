package com.brinvex.fintracker.connector.ibkr.impl;


import com.brinvex.fintracker.connector.ibkr.api.model.IbkrDocKey.ActivityDocKey;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrDocKey.TradeConfirmDocKey;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.AssetCategory;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.AssetSubCategory;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.FlexStatement;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.FlexStatement.ActivityStatement;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrDms;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrStatementParser;
import com.brinvex.fintracker.connector.ibkr.impl.service.IbkrDmsImpl;
import com.brinvex.fintracker.connector.ibkr.impl.service.IbkrStatementParserImpl;
import com.brinvex.fintracker.testsupport.TestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class IbkrStatementParserTest {

    private static final Logger LOG = LoggerFactory.getLogger(IbkrStatementParserTest.class);

    private static final TestSupport testSupport = new TestSupport("connector-ibkr");

    private static final String ibkrTestAccount1 = testSupport.property("ibkrTestAccount1");

    private static boolean ibkrTestAccount1IsNotNull() {
        return ibkrTestAccount1 != null;
    }

    @EnabledIf("ibkrTestAccount1IsNotNull")
    @Test
    void parseActivity() {
        IbkrDms dms = new IbkrDmsImpl(testSupport.dmsFactory().getDms("dms-pers1"));
        List<ActivityDocKey> docKeys = dms.getActivityDocKeys(ibkrTestAccount1, null, null);
        assertFalse(docKeys.isEmpty());
        IbkrStatementParser parser = new IbkrStatementParserImpl();
        for (ActivityDocKey docKey : docKeys) {
            LOG.debug("parseActivity - {}", docKey);

            String content = dms.getStatementContent(docKey);
            ActivityStatement activityStatement = parser.parseActivityStatement(content);
            assertNotNull(activityStatement);

            int days = (int) ChronoUnit.DAYS.between(activityStatement.fromDate(), activityStatement.toDate());
            int equitySummariesSize = activityStatement.equitySummaries().size();
            assertTrue(equitySummariesSize >= 1);
            assertTrue(equitySummariesSize <= days);

            try {
                parser.parseTradeConfirmStatement(content);
                fail();
            } catch (Exception expected) {
            }
        }
    }

    @EnabledIf("ibkrTestAccount1IsNotNull")
    @Test
    void parseTradeConfirm() {
        IbkrDms dms = new IbkrDmsImpl(testSupport.dmsFactory().getDms("dms-pers2"));
        List<TradeConfirmDocKey> docKeys = dms.getTradeConfirmDocKeys(ibkrTestAccount1, null, null);
        assertFalse(docKeys.isEmpty());
        IbkrStatementParser parser = new IbkrStatementParserImpl();
        for (TradeConfirmDocKey docKey : docKeys) {
            LOG.debug("parseTradeConfirm - {}", docKey);

            String content = dms.getStatementContent(docKey);
            FlexStatement.TradeConfirmStatement tradeConfirmStatement = parser.parseTradeConfirmStatement(content);
            assertNotNull(tradeConfirmStatement);
            tradeConfirmStatement.tradeConfirmations()
                    .forEach(tc -> {
                        AssetCategory cat = tc.assetCategory();
                        AssetSubCategory subCat = tc.assetSubCategory();
                        assertTrue(AssetCategory.STK.equals(cat) && AssetSubCategory.COMMON.equals(subCat)
                                   || AssetCategory.CASH.equals(cat) && null == subCat
                        );
                    });

            try {
                parser.parseActivityStatement(content);
                fail();
            } catch (Exception expected) {
            }
        }
    }
}

