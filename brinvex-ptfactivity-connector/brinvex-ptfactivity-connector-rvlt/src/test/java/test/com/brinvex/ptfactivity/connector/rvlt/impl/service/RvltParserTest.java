package test.com.brinvex.ptfactivity.connector.rvlt.impl.service;

import com.brinvex.ptfactivity.connector.rvlt.api.RvltModule;
import com.brinvex.ptfactivity.connector.rvlt.api.model.RvltDocKey.PnlStatementDocKey;
import com.brinvex.ptfactivity.connector.rvlt.api.model.RvltDocKey.TradingAccountStatementDocKey;
import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.PnlStatement;
import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.TradingAccountStatement;
import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.Transaction;
import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.TransactionType;
import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltDms;
import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltStatementParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.brinvex.java.DateUtil.isFirstDayOfMonth;
import static java.math.BigDecimal.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RvltParserTest extends RvltBaseTest {

    private final RvltModule rvltModule = testCtx.withDmsWorkspace("rvlt-dms-stable").get(RvltModule.class);

    @EnabledIf("account1IsNotNull")
    @Test
    void parseTradingAccountStatement() {
        assert account1 != null;

        RvltDms dms = rvltModule.dms();
        String accountNumber = account1.externalId();
        String accountName = account1.name();
        List<TradingAccountStatementDocKey> docKeys = dms.getTradingAccountStatementDocKeys(accountNumber, null, null);
        assertFalse(docKeys.isEmpty());
        RvltStatementParser parser = rvltModule.statementParser();
        Set<Transaction> transactions = new LinkedHashSet<>();
        for (TradingAccountStatementDocKey docKey : docKeys) {
            LOG.debug("parseTradingAccountStatement - {}", docKey);

            byte[] content = dms.getStatementContent(docKey);
            LOG.debug("tradingAccountStatement #content - {}", content.length);

            TradingAccountStatement accStatement = parser.parseTradingAccountStatement(content);
            assertNotNull(accStatement);

            assertEquals(accountNumber, accStatement.accountNumber());
            if (accountName != null) {
                assertEquals(accountName, accStatement.accountName());
            }

            assertEquals(accStatement.startCashValue().add(accStatement.startStocksValue()).compareTo(accStatement.startValue()), 0);
            assertEquals(accStatement.endCashValue().add(accStatement.endStocksValue()).compareTo(accStatement.endValue()), 0);

            assertFalse(accStatement.transactions().isEmpty());
            for (Transaction transaction : accStatement.transactions()) {
                LocalDate tranDate = transaction.date().toLocalDate();
                if (isFirstDayOfMonth(tranDate) && tranDate.isEqual(docKey.toDateIncl())) {
                    continue;
                }
                assertNotNull(transaction.date(), transaction::toString);
                assertNotNull(transaction.ccy(), transaction::toString);
                assertNotNull(transaction.type(), transaction::toString);

                assertTrue(transactions.add(transaction), transaction::toString);
            }
        }
    }

    @EnabledIf("account1IsNotNull")
    @Test
    void parsePnlStatement() {
        assert account1 != null;
        String accountNumber = account1.externalId();
        String accountName = account1.name();

        RvltDms dms = rvltModule.dms();
        List<PnlStatementDocKey> docKeys = dms.getPnlStatementDocKeys(account1.externalId(), null, null);
        assertFalse(docKeys.isEmpty());
        RvltStatementParser parser = rvltModule.statementParser();

        BigDecimal tolerance = new BigDecimal("0.01");
        BigDecimal taxRatioMin = new BigDecimal("0.01");
        BigDecimal taxRatioMax = new BigDecimal("0.45");

        for (PnlStatementDocKey docKey : docKeys) {
            LOG.debug("parsePnlStatement - {}", docKey);

            byte[] content = dms.getStatementContent(docKey);
            LOG.debug("pnlStatement #content - {}", content.length);

            PnlStatement pnlStatement = parser.parsePnlStatement(content);
            assertNotNull(pnlStatement);

            assertEquals(accountNumber, pnlStatement.accountNumber());
            if (accountName != null) {
                assertEquals(accountName, pnlStatement.accountName());
            }

            assertFalse(pnlStatement.transactions().isEmpty());
            LOG.debug("Testing {} pnlTransactions", pnlStatement.transactions().size());
            for (Transaction transaction : pnlStatement.transactions()) {
                assertNotNull(transaction.date(), transaction::toString);
                assertNotNull(transaction.ccy(), transaction::toString);
                assertNotNull(transaction.symbol(), transaction::toString);
                assertNotNull(transaction.securityName(), transaction::toString);
                assertNotNull(transaction.country(), transaction::toString);
                assertEquals(TransactionType.DIVIDEND, transaction.type(), transaction::toString);
                assertTrue(transaction.grossAmount().compareTo(ZERO) > 0, transaction::toString);
                assertTrue(transaction.withholdingTax().compareTo(ZERO) >= 0, transaction::toString);
                assertTrue(transaction.value().compareTo(ZERO) >= 0, transaction::toString);
                assertNotNull(transaction.isin(), transaction::toString);

                BigDecimal calcNetValue = transaction.grossAmount().subtract(transaction.withholdingTax());
                assertTrue(calcNetValue.subtract(transaction.value()).abs().compareTo(tolerance) < 0, transaction::toString);

                if (transaction.withholdingTax().compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal taxPct = transaction.withholdingTax().divide(transaction.grossAmount(), 2, RoundingMode.HALF_UP);
                    assertTrue(taxPct.compareTo(taxRatioMin) > 0, transaction::toString);
                    assertTrue(taxPct.compareTo(taxRatioMax) < 0, transaction::toString);
                }
            }
        }
    }

}
