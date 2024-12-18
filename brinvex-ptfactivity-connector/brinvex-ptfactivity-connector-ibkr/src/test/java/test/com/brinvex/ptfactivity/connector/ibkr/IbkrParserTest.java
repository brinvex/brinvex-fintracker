package test.com.brinvex.ptfactivity.connector.ibkr;


import com.brinvex.ptfactivity.connector.ibkr.api.IbkrModule;
import com.brinvex.ptfactivity.connector.ibkr.api.model.IbkrDocKey.ActivityDocKey;
import com.brinvex.ptfactivity.connector.ibkr.api.model.IbkrDocKey.TradeConfirmDocKey;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.AssetCategory;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.AssetSubCategory;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CashTransaction;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.EquitySummary;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.FlexStatement.ActivityStatement;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.FlexStatement.TradeConfirmStatement;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrDms;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrStatementParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.math.BigDecimal.ZERO;
import static java.time.LocalDate.parse;
import static java.time.ZoneId.systemDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class IbkrParserTest extends IbkrBaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(IbkrParserTest.class);

    @EnabledIf("account1IsNotNull")
    @Test
    void parseActivity() {
        assert account1 != null;

        IbkrModule ibkrModule = testCtx.withDmsWorkspace("ibkr-dms-stable").get(IbkrModule.class);
        IbkrDms dms = ibkrModule.dms();
        List<ActivityDocKey> docKeys = dms.getActivityDocKeys(account1.externalId(), null, null);
        assertFalse(docKeys.isEmpty());
        IbkrStatementParser parser = ibkrModule.statementParser();
        for (ActivityDocKey docKey : docKeys) {
            LOG.debug("parseActivity - {}", docKey);

            String content = dms.getStatementContent(docKey);
            ActivityStatement activityStatement = parser.parseActivityStatement(content);
            assertNotNull(activityStatement);

            int days = (int) ChronoUnit.DAYS.between(activityStatement.fromDate(), activityStatement.toDate());
            int equitySummariesSize = activityStatement.equitySummaries().size();
            assertTrue(equitySummariesSize >= 1);
            assertTrue(equitySummariesSize <= days);

            for (CashTransaction cashTran : activityStatement.cashTransactions()) {
                assertTrue(cashTran.fxRateToBase().compareTo(ZERO) > 0);
            }

            try {
                parser.parseTradeConfirmStatement(content);
                fail();
            } catch (Exception expected) {
            }
        }
    }

    @EnabledIf("account1IsNotNull")
    @Test
    void parseActivityCreatedOn() {
        assert account1 != null;

        IbkrModule ibkrModule = testCtx.withDmsWorkspace("ibkr-dms-stable").get(IbkrModule.class);
        IbkrDms dms = ibkrModule.dms();
        List<ActivityDocKey> docKeys = dms.getActivityDocKeys(account1.externalId(), null, null);
        assertFalse(docKeys.isEmpty());
        IbkrStatementParser parser = ibkrModule.statementParser();
        for (ActivityDocKey docKey : docKeys) {
            LOG.debug("parseActivityCreatedOn - {}", docKey);

            String content = dms.getStatementContent(docKey);
            ActivityStatement activityStatement = parser.parseActivityStatement(content);
            assertNotNull(activityStatement);

            LocalDateTime createdOn = parser.parseStatementCreatedOn(dms.getStatementContentLines(docKey, 3));
            assertEquals(activityStatement.whenGenerated().withZoneSameInstant(systemDefault()).toLocalDateTime(), createdOn);
        }
    }

    @EnabledIf("account1IsNotNull")
    @Test
    void parseTradeConfirm() {
        assert account1 != null;

        IbkrModule ibkrModule = testCtx.withDmsWorkspace("ibkr-dms-stable-20240418").get(IbkrModule.class);
        IbkrDms dms = ibkrModule.dms();
        List<TradeConfirmDocKey> docKeys = dms.getTradeConfirmDocKeys(account1.externalId(), null, null);
        assertFalse(docKeys.isEmpty());
        IbkrStatementParser parser = ibkrModule.statementParser();
        for (TradeConfirmDocKey docKey : docKeys) {
            LOG.debug("parseTradeConfirm - {}", docKey);

            String content = dms.getStatementContent(docKey);
            TradeConfirmStatement tradeConfirmStatement = parser.parseTradeConfirmStatement(content);
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

    @EnabledIf("account1IsNotNull")
    @Test
    void parseEquitySummaries() {
        assert account1 != null;

        IbkrModule ibkrModule = testCtx.withDmsWorkspace("ibkr-dms-stable").get(IbkrModule.class);
        IbkrDms dms = ibkrModule.dms();
        IbkrStatementParser parser = ibkrModule.statementParser();
        ActivityDocKey docKey = new ActivityDocKey(account1.externalId(), parse("2023-01-23"), parse("2024-01-23"));
        String content = dms.getStatementContent(docKey);
        ActivityStatement actStatement = parser.parseActivityStatement(content);
        assertNotNull(actStatement);

        List<EquitySummary> equitySummaries = actStatement.equitySummaries();
        assertEquals(0, equitySummaries.getFirst().total().compareTo(ZERO));

        BigDecimal tolerance = new BigDecimal("0.001");
        for (EquitySummary eqSummary : equitySummaries) {
            BigDecimal cash = eqSummary.cash();
            BigDecimal stock = eqSummary.stock();
            BigDecimal dividendAccruals = eqSummary.dividendAccruals();
            BigDecimal interestAccruals = eqSummary.interestAccruals();
            BigDecimal total = eqSummary.total();
            BigDecimal sum = cash.add(stock).add(dividendAccruals).add(interestAccruals);
            if (total.subtract(sum).abs().compareTo(tolerance) > 0) {
                fail("total=%s != sum=%s, %s".formatted(total, sum, eqSummary));
            }
        }

        {
            EquitySummary newestEqSummary = equitySummaries.getLast();
            assertTrue(newestEqSummary.reportDate().isEqual(LocalDate.parse("2024-01-23")));
        }
        {
            LocalDate someDate = parse("2023-08-02");
            EquitySummary someEqSummary = equitySummaries
                    .stream()
                    .filter(e -> e.reportDate().isEqual(someDate))
                    .findAny()
                    .orElseThrow();

            BigDecimal total = new BigDecimal("15899.966794514");
            BigDecimal cash = new BigDecimal("44.027090414");
            BigDecimal stock = new BigDecimal("15843.9724334");
            BigDecimal dividendAccruals = new BigDecimal("11.9672707");
            assertEquals(0, someEqSummary.cash().compareTo(cash));
            assertEquals(0, someEqSummary.total().compareTo(total));
            assertEquals(0, someEqSummary.stock().compareTo(stock));
            assertEquals(0, someEqSummary.dividendAccruals().compareTo(dividendAccruals));
            assertEquals(0, cash.add(stock).add(dividendAccruals).compareTo(total));
        }
    }
}

