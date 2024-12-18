package com.brinvex.ptfactivity.connector.rvlt.internal.service.parser;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.PnlStatement;
import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.Transaction;
import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.TransactionType;
import com.brinvex.java.validation.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.brinvex.ptfactivity.connector.rvlt.internal.service.parser.RvltParsingUtil.parseMoney;
import static com.brinvex.java.StringUtil.stripToEmpty;


@SuppressWarnings("DuplicatedCode")
class RvltPnlStatementParser {

    private static class Lazy {

        private static final Pattern ACCOUNT_NAME_PATTERN = Pattern.compile(
                "Account\\s+name\\s+(?<accountName>.+)");

        private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile(
                "Account\\s+number\\s+(?<accountNumber>.+)");

        private static final Pattern PERIOD_PATTERN = Pattern.compile(
                "Period\\s+(?<periodFrom>\\d{2}\\s[A-Za-z]{3}\\s\\d{4})\\s-\\s(?<periodTo>\\d{2}\\s[A-Za-z]{3}\\s\\d{4})");

        private static final Pattern TRANSACTION_HEADER_PATTERN = Pattern.compile(
                "Date\\s+Symbol\\s+Security\\s+name\\s+ISIN\\s+Country\\s+Gross\\s+Amount\\s+Withholding\\s+Tax\\s+Net\\s+Amount");

        private static final Pattern TRANSACTION_FOOTER_PATTERN = Pattern.compile(
                "Total\\s+.*");

        private static final Pattern DIVIDEND_START_LINE_PATTERN1 = Pattern.compile(
                "(?<date>\\d{4}-\\d{2}-\\d{2})" +
                "\\s+(?<symbol>\\S+)" +
                "\\s+(?<securityName>.+)" +
                "\\s+(?<isin>\\S{12})" +
                "\\s+(?<country>\\S{2}+)" +
                "\\s+US(?<grossAmount>-?\\$(\\d+,)*\\d+(\\.\\d+)?)" +
                "\\s+US(?<tax>-?\\$(\\d+,)*\\d+(\\.\\d+)?)" +
                "\\s+US(?<netAmount>-?\\$(\\d+,)*\\d+(\\.\\d+)?)"
        );

        private static final Pattern DIVIDEND_START_LINE_PATTERN2 = Pattern.compile(
                "(?<date>\\d{4}-\\d{2}-\\d{2})" +
                "\\s+(?<symbol>\\S+)" +
                "\\s+(?<securityName>.+)" +
                "\\s+(?<isin>\\S{12})" +
                "\\s+(?<country>\\S{2}+)" +
                "\\s+US(?<grossAmount>-?\\$(\\d+,)*\\d+(\\.\\d+)?)" +
                "\\s+-" +
                "\\s+US(?<netAmount>-?\\$(\\d+,)*\\d+(\\.\\d+)?)"
        );

        private static final Pattern DIVIDEND_START_LINE_PATTERN3 = Pattern.compile(
                "(?<date>\\d{4}-\\d{2}-\\d{2})" +
                "\\s+(?<symbol>\\S+)" +
                "\\s+(?<securityName>.+)" +
                "\\s+(?<isin>\\S{12})" +
                "\\s+(?<country>\\S{2}+)" +
                "\\s+US(?<grossAmount>-?\\$(\\d+,)*\\d+(\\.\\d+)?)"
        );

        private static final DateTimeFormatter PERIOD_DF = DateTimeFormatter.ofPattern("dd MMM yyyy");
    }

    public PnlStatement parseAccountStatement(List<String> lines) {
        int r = 0;
        int linesSize = lines.size();
        {
            boolean eurHeader1Found = false;
            for (; r < linesSize; r++) {
                String line = stripToEmpty(lines.get(r));
                if (line.equals("EUR Profit and Loss Statement")) {
                    eurHeader1Found = true;
                    r++;
                    break;
                }
            }
            Assert.isTrue(eurHeader1Found);
        }
        {
            boolean eurSellsFound = false;
            for (; r < linesSize; r++) {
                String line = stripToEmpty(lines.get(r));
                if (line.equals("Sells")) {
                    eurSellsFound = true;
                    r++;
                    break;
                }
            }
            Assert.isTrue(eurSellsFound);
        }
        {
            boolean eurHeader2Found = false;
            for (; r < linesSize; r++) {
                String line = stripToEmpty(lines.get(r));
                if (line.equals("EUR Profit and Loss Statement")) {
                    eurHeader2Found = true;
                    r++;
                    break;
                }
            }
            Assert.isTrue(eurHeader2Found);
        }
        {
            boolean eurIncomesFound = false;
            for (; r < linesSize; r++) {
                String line = stripToEmpty(lines.get(r));
                if (line.equals("Other income & fees")) {
                    eurIncomesFound = true;
                    r++;
                    break;
                }
            }
            Assert.isTrue(eurIncomesFound);
        }
        {
            boolean usdHeader1Found = false;
            for (; r < linesSize; r++) {
                String line = stripToEmpty(lines.get(r));
                if (line.equals("USD Profit and Loss Statement")) {
                    usdHeader1Found = true;
                    r++;
                    break;
                }
            }
            Assert.isTrue(usdHeader1Found);
        }

        LocalDate periodFrom = null;
        LocalDate periodTo = null;
        {
            for (; r < linesSize; r++) {
                String line = stripToEmpty(lines.get(r));
                Matcher matcher = Lazy.PERIOD_PATTERN.matcher(line);
                if (matcher.find()) {
                    periodFrom = LocalDate.parse(matcher.group("periodFrom"), Lazy.PERIOD_DF);
                    periodTo = LocalDate.parse(matcher.group("periodTo"), Lazy.PERIOD_DF);
                    r++;
                    break;
                }
            }
            Assert.notNull(periodFrom);
            Assert.notNull(periodTo);
        }

        String accountName = null;
        {
            for (; r < linesSize; r++) {
                String line = stripToEmpty(lines.get(r));
                Matcher matcher = Lazy.ACCOUNT_NAME_PATTERN.matcher(line);
                if (matcher.find()) {
                    accountName = matcher.group("accountName");
                    r++;
                    break;
                }
            }
            Assert.notNull(accountName);
        }
        String accountNumber = null;
        {
            for (; r < linesSize; r++) {
                String line = stripToEmpty(lines.get(r));
                Matcher matcher = Lazy.ACCOUNT_NUMBER_PATTERN.matcher(line);
                if (matcher.find()) {
                    accountNumber = matcher.group("accountNumber");
                    r++;
                    break;
                }
            }
            Assert.notNull(accountNumber);
        }

        {
            boolean usdSellsFound = false;
            for (; r < linesSize; r++) {
                String line = stripToEmpty(lines.get(r));
                if (line.equals("Sells")) {
                    usdSellsFound = true;
                    r++;
                    break;
                }
            }
            Assert.isTrue(usdSellsFound);
        }
        {
            boolean usdIncomesFound = false;
            for (; r < linesSize; r++) {
                String line = stripToEmpty(lines.get(r));
                if (line.equals("Other income & fees")) {
                    usdIncomesFound = true;
                    r++;
                    break;
                }
            }
            Assert.isTrue(usdIncomesFound);
        }
        {
            String line = stripToEmpty(lines.get(r));
            Assert.isTrue(Lazy.TRANSACTION_HEADER_PATTERN.matcher(line).matches());
            r++;
        }

        List<String> transactionLines = lines.stream()
                .skip(r)
                .takeWhile(line -> !Lazy.TRANSACTION_FOOTER_PATTERN.matcher(line).matches())
                .filter(line -> !Lazy.TRANSACTION_HEADER_PATTERN.matcher(line).matches())
                .toList();
        List<Transaction> dividTrans = parseDividendTransactions(transactionLines, Currency.USD, "US");

        return new PnlStatement(
                accountName,
                accountNumber,
                periodFrom,
                periodTo,
                Currency.USD,
                dividTrans
        );
    }

    @SuppressWarnings("SameParameterValue")
    private List<Transaction> parseDividendTransactions(List<String> lines, Currency ccy, String marketCountry) {

        List<Transaction> dividTrans = new ArrayList<>();
        for (int tr = 0, transactionLinesSize = lines.size(); tr < transactionLinesSize; tr++) {
            String line = stripToEmpty(lines.get(tr));
            if (line.isBlank()) {
                continue;
            }
            Transaction.TransactionBuilder dividendTran = Transaction.builder();
            try {
                Matcher matcher = Lazy.DIVIDEND_START_LINE_PATTERN1.matcher(line);
                if (matcher.find()) {
                    dividendTran.date(LocalDate.parse(matcher.group("date")).atStartOfDay(ZoneId.of("GMT")).withFixedOffsetZone());
                    dividendTran.symbol(matcher.group("symbol"));
                    dividendTran.securityName(matcher.group("securityName"));
                    dividendTran.isin(matcher.group("isin"));
                    dividendTran.country(matcher.group("country"));
                    dividendTran.qty(null);
                    dividendTran.price(null);
                    dividendTran.grossAmount(parseMoney(matcher.group("grossAmount")));
                    dividendTran.withholdingTax(parseMoney(matcher.group("tax")));
                    dividendTran.value(parseMoney(matcher.group("netAmount")));

                    tr = tr + 3;

                } else {
                    matcher = Lazy.DIVIDEND_START_LINE_PATTERN2.matcher(line);
                    if (matcher.find()) {
                        dividendTran.date(LocalDate.parse(matcher.group("date")).atStartOfDay(ZoneId.of("GMT")).withFixedOffsetZone());
                        dividendTran.symbol(matcher.group("symbol"));
                        dividendTran.securityName(matcher.group("securityName"));
                        dividendTran.isin(matcher.group("isin"));
                        dividendTran.country(matcher.group("country"));
                        dividendTran.qty(null);
                        dividendTran.price(null);
                        dividendTran.grossAmount(parseMoney(matcher.group("grossAmount")));
                        dividendTran.withholdingTax(BigDecimal.ZERO);
                        dividendTran.value(parseMoney(matcher.group("netAmount")));

                        tr = tr + 3;
                    } else {
                        matcher = Lazy.DIVIDEND_START_LINE_PATTERN3.matcher(line);
                        if (!matcher.find()) {
                            throw new IllegalStateException("Pattern not found: " + line);
                        }
                        dividendTran.date(LocalDate.parse(matcher.group("date")).atStartOfDay(ZoneId.of("GMT")).withFixedOffsetZone());
                        dividendTran.symbol(matcher.group("symbol"));
                        dividendTran.securityName(matcher.group("securityName"));
                        dividendTran.isin(matcher.group("isin"));
                        dividendTran.country(matcher.group("country"));
                        dividendTran.qty(null);
                        dividendTran.price(null);
                        dividendTran.grossAmount(parseMoney(matcher.group("grossAmount")));

                        String taxLine;
                        String valueLine;
                        if (lines.get(tr + 2).startsWith("Rate:")) {
                            taxLine = lines.get(tr + 3);
                            valueLine = lines.get(tr + 5);
                        } else {
                            taxLine = lines.get(tr + 2);
                            valueLine = lines.get(tr + 4);
                        }
                        tr = tr + 6;
                        if (taxLine.isBlank() || taxLine.equals("-")) {
                            dividendTran.withholdingTax(BigDecimal.ZERO);
                        } else {
                            dividendTran.withholdingTax(parseMoney(taxLine));
                        }
                        dividendTran.value(parseMoney(valueLine));
                    }
                }

                dividendTran.ccy(ccy);
                dividendTran.country(marketCountry);
                dividendTran.type(TransactionType.DIVIDEND);
                dividendTran.fees(null);
                dividendTran.commission(null);
                dividTrans.add(dividendTran.build());

            } catch (Exception e) {
                throw new IllegalStateException("Exception while parsing line: tr=%s, '%s'".formatted(tr, line), e);
            }
        }
        return dividTrans;
    }


}
