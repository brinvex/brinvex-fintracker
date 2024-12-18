package test.com.brinvex.ptfactivity.connector.fiob;


import com.brinvex.ptfactivity.connector.fiob.api.FiobModule;
import com.brinvex.ptfactivity.connector.fiob.api.model.FiobDocKey.TradingSnapshotDocKey;
import com.brinvex.ptfactivity.connector.fiob.api.model.FiobDocKey.TradingTransDocKey;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.Lang;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.Statement.TradingTransStatement;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.TradingTransaction;
import com.brinvex.ptfactivity.connector.fiob.api.service.FiobDms;
import com.brinvex.ptfactivity.connector.fiob.api.service.FiobStatementParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.time.LocalDate.parse;
import static java.util.Comparator.comparing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiobParserTest extends FiobBaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(FiobParserTest.class);

    @EnabledIf("account1IsNotNull")
    @Test
    void parseTradingTransStatement1() {
        assert account1 != null;
        FiobModule fiobModule = testCtx.withDmsWorkspace("fiob-dms-stable-20231231").get(FiobModule.class);
        FiobDms dms = fiobModule.dms();
        List<TradingTransDocKey> docKeys = dms.getTradingTransDocKeys(account1.externalId(), null, null);
        assertFalse(docKeys.isEmpty());
        FiobStatementParser parser = fiobModule.statementParser();

        for (TradingTransDocKey docKey : docKeys) {
            String content = dms.getStatementContent(docKey);
            TradingTransStatement tradingTransStatement = parser.parseTradingTransStatement(content);
            assertNotNull(tradingTransStatement);

            List<TradingTransaction> transactions = tradingTransStatement.transactions();
            assertFalse(transactions.isEmpty());
        }
    }

    @EnabledIf("account1IsNotNull")
    @Test
    void parseTradingTransStatement2_lang() {
        assert account1 != null;

        TradingTransStatement statementsSK;
        {
            FiobModule fiobModule = testCtx.withDmsWorkspace("fiob-dms-stable-20221231_SK").get(FiobModule.class);
            FiobStatementParser statementParser = fiobModule.statementParser();
            FiobDms dms = fiobModule.dms();
            statementsSK = fiobModule.statementMerger().mergeTradingTransStatements(
                            dms.getTradingTransDocKeys(account1.externalId(), null, null)
                                    .stream()
                                    .map(dms::getStatementContent)
                                    .map(statementParser::parseTradingTransStatement)
                                    .toList())
                    .orElseThrow();
        }
        TradingTransStatement statementCZ;
        {
            FiobModule fiobModule = testCtx.withDmsWorkspace("fiob-dms-stable-20221231_CZ").get(FiobModule.class);
            FiobStatementParser statementParser = fiobModule.statementParser();
            FiobDms dms = fiobModule.dms();
            statementCZ = fiobModule.statementMerger().mergeTradingTransStatements(
                            dms.getTradingTransDocKeys(account1.externalId(), null, null)
                                    .stream()
                                    .map(dms::getStatementContent)
                                    .map(statementParser::parseTradingTransStatement)
                                    .toList())
                    .orElseThrow();
        }
        TradingTransStatement statementEN;
        {
            FiobModule fiobModule = testCtx.withDmsWorkspace("fiob-dms-stable-20221231_EN").get(FiobModule.class);
            FiobStatementParser statementParser = fiobModule.statementParser();
            FiobDms dms = fiobModule.dms();
            statementEN = fiobModule.statementMerger().mergeTradingTransStatements(
                            dms.getTradingTransDocKeys(account1.externalId(), null, null)
                                    .stream()
                                    .map(dms::getStatementContent)
                                    .map(statementParser::parseTradingTransStatement)
                                    .toList())
                    .orElseThrow();
        }

        Map<TradingTransStatement, Lang> tranLists = Map.of(statementCZ, Lang.CZ, statementEN, Lang.EN);

        for (Map.Entry<TradingTransStatement, Lang> e : tranLists.entrySet()) {
            TradingTransStatement tranList = e.getKey();
            Lang lang = e.getValue();
            assertEquals(statementsSK.periodFrom(), tranList.periodFrom());
            assertEquals(statementsSK.periodTo(), tranList.periodTo());
            assertEquals(statementsSK.accountId(), tranList.accountId());

            List<TradingTransaction> trans = tranList.transactions();
            for (int j = 0, transSize = trans.size(); j < transSize; j++) {
                TradingTransaction tran0 = statementsSK.transactions().get(j);
                TradingTransaction tran = trans.get(j);
                String assetMsg = "\nSK tran=%s,\n%s tran=%s".formatted(tran0, lang, tran);

                assertEquals(tran0.tradeDate(), tran.tradeDate());
                assertEquals(tran0.direction(), tran.direction());
                assertEquals(tran0.symbol(), tran.symbol());
                assertEquals(tran0.rawSymbol(), tran.rawSymbol(), assetMsg);
                assertEquals(tran0.price(), tran.price());
                assertEquals(tran0.shares(), tran.shares());
                assertEquals(tran0.rawCcy(), tran.rawCcy());
                assertEquals(tran0.volumeCzk(), tran.volumeCzk());
                assertEquals(tran0.feesCzk(), tran.feesCzk());
                assertEquals(tran0.volumeUsd(), tran.volumeUsd());
                assertEquals(tran0.feesUsd(), tran.feesUsd());
                assertEquals(tran0.volumeEur(), tran.volumeEur());
                assertEquals(tran0.feesEur(), tran.feesEur());
                assertTrue(tran0.instrumentName() == null || tran.instrumentName() == null || tran0.instrumentName().equals(tran.instrumentName()));
                assertEquals(tran0.settleDate(), tran.settleDate());
                assertEquals(tran0.orderId(), tran.orderId());
                assertEquals(tran0.userComments(), tran.userComments(), assetMsg);

            }
        }
    }

    @EnabledIf("account1IsNotNull")
    @Test
    void parseTradingTransStatement2_sort() {
        assert account1 != null;
        FiobModule fiobModule = testCtx.withDmsWorkspace("fiob-dms-stable-20231231").get(FiobModule.class);
        FiobDms dms = fiobModule.dms();
        FiobStatementParser parser = fiobModule.statementParser();

        TradingTransStatement statements = fiobModule.statementMerger().mergeTradingTransStatements(
                        dms.getTradingTransDocKeys(account1.externalId(), null, null)
                                .stream()
                                .map(dms::getStatementContent)
                                .map(parser::parseTradingTransStatement)
                                .toList())
                .orElseThrow();
        List<TradingTransaction> rawTrans = statements.transactions();
        List<TradingTransaction> sortedTransactions = rawTrans
                .stream()
                .sorted(comparing(TradingTransaction::tradeDate))
                .collect(Collectors.toList());
        assertEquals(sortedTransactions, rawTrans);
    }

    @EnabledIf("account1IsNotNull")
    @Test
    void parseTradingStatement_createdOn() {
        assert account1 != null;
        FiobModule fiobModule = testCtx.withDmsWorkspace("fiob-dms-stable-20231231").get(FiobModule.class);
        FiobDms dms = fiobModule.dms();
        FiobStatementParser parser = fiobModule.statementParser();

        {
            LocalDateTime createdOn = parser.parseTradingStatementCreatedOn(
                    dms.getStatementContentLinesIfExists(new TradingSnapshotDocKey(account1.externalId(), parse("2023-12-31")), 2)
            );
            assertEquals(LocalDateTime.parse("2024-06-13T12:00:48"), createdOn);
        }
        {
            LocalDateTime createdOn = parser.parseTradingStatementCreatedOn(
                    dms.getStatementContentLinesIfExists(new TradingTransDocKey(account1.externalId(), parse("2023-01-01"), parse("2023-12-31")), 2)
            );
            assertEquals(LocalDateTime.parse("2024-02-10T23:01:11"), createdOn);
        }
    }
}
