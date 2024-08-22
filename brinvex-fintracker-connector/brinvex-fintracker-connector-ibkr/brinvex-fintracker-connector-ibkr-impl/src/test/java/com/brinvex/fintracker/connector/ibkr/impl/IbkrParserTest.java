package com.brinvex.fintracker.connector.ibkr.impl;


import com.brinvex.fintracker.connector.ibkr.api.model.IbkrDocKey.ActivityDocKey;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrDocKey.TradeConfirmDocKey;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.AssetCategory;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.AssetSubCategory;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.EquitySummary;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.math.BigDecimal.ZERO;
import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class IbkrParserTest {

    private static final Logger LOG = LoggerFactory.getLogger(IbkrParserTest.class);

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

    @EnabledIf("ibkrTestAccount1IsNotNull")
    @Test
    void parseEquitySummaries() {
        IbkrDms dms = new IbkrDmsImpl(testSupport.dmsFactory().getDms("dms-pers1"));
        IbkrStatementParser parser = new IbkrStatementParserImpl();
        ActivityDocKey docKey = new ActivityDocKey(ibkrTestAccount1, parse("2022-08-03"), parse("2023-08-02"));
        String content = dms.getStatementContent(docKey);
        ActivityStatement actStatement = parser.parseActivityStatement(content);
        assertNotNull(actStatement);

        List<EquitySummary> equitySummaries = actStatement.equitySummaries();
        assertEquals(0, equitySummaries.getFirst().total().compareTo(ZERO));

        for (EquitySummary eqSummary : equitySummaries) {
            BigDecimal cash = eqSummary.cash();
            BigDecimal stock = eqSummary.stock();
            BigDecimal dividendAccruals = eqSummary.dividendAccruals();
            BigDecimal interestAccruals = eqSummary.interestAccruals();
            BigDecimal total = eqSummary.total();
            BigDecimal sum = cash.add(stock).add(dividendAccruals).add(interestAccruals);
            if (total.subtract(sum).abs().compareTo(new BigDecimal("0.001")) > 0) {
                fail("total=%s != sum=%s, %s".formatted(total, sum, eqSummary));
            }
        }

        EquitySummary newestEqSummary = equitySummaries.getLast();
        assertTrue(newestEqSummary.reportDate().isEqual(LocalDate.parse("2023-08-02")));

        BigDecimal total = new BigDecimal("15899.966794514");
        BigDecimal cash = new BigDecimal("44.027090414");
        BigDecimal stock = new BigDecimal("15843.9724334");
        BigDecimal dividendAccruals = new BigDecimal("11.9672707");
        assertEquals(0, newestEqSummary.cash().compareTo(cash));
        assertEquals(0, newestEqSummary.total().compareTo(total));
        assertEquals(0, newestEqSummary.stock().compareTo(stock));
        assertEquals(0, newestEqSummary.dividendAccruals().compareTo(dividendAccruals));
        assertEquals(0, cash.add(stock).add(dividendAccruals).compareTo(total));
    }
}

