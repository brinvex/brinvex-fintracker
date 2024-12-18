package com.brinvex.ptfactivity.connector.rvlt.internal.service.parser;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.TradingAccountStatement;
import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.Transaction;
import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.TransactionSide;
import com.brinvex.ptfactivity.connector.rvlt.api.model.statement.TransactionType;
import com.brinvex.java.validation.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.brinvex.ptfactivity.connector.rvlt.internal.service.parser.RvltParsingUtil.parseDecimal;
import static com.brinvex.ptfactivity.connector.rvlt.internal.service.parser.RvltParsingUtil.parseMoney;
import static com.brinvex.java.StringUtil.stripToEmpty;


@SuppressWarnings("DuplicatedCode")
class RvltTradingAccStatementParser {

    private static class Lazy {

        private static final Pattern ACCOUNT_NAME_PATTERN = Pattern.compile(
                "Account\\s+name\\s+(?<accountName>.+)");

        private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile(
                "Account\\s+number\\s+(?<accountNumber>.+)");

        private static final Pattern PERIOD_PATTERN = Pattern.compile(
                "Period\\s+(?<periodFrom>\\d{2}\\s[A-Za-z]{3}\\s\\d{4})\\s-\\s(?<periodTo>\\d{2}\\s[A-Za-z]{3}\\s\\d{4})");

        private static final Pattern TRANSACTIONS_HEADER_PATTERN = Pattern.compile(
                "Date\\s*Symbol\\s*Type\\s*Quantity\\s*Price\\s*Side\\s*Value\\s*Fees\\s*Commission");

        private static final Pattern ACC_SUMMARY_HEADER_PATTERN = Pattern.compile(
                "Starting\\s+Ending");
        private static final Pattern ACC_SUMMARY_STOCKS_VALUE_PATTERN = Pattern.compile(
                "Stocks\\s+value\\s+(?<startValue>-?(US)?\\$(\\d+,)*\\d+(\\.\\d+)?)\\s+(?<endValue>-?(US)?\\$(\\d+,)*\\d+(\\.\\d+)?)");
        private static final Pattern ACC_SUMMARY_CASH_VALUE_PATTERN = Pattern.compile(
                "Cash\\s+value\\s*\\*?\\s+(?<startValue>-?(US)?\\$(\\d+,)*\\d+(\\.\\d+)?)\\s+(?<endValue>-?(US)?\\$(\\d+,)*\\d+(\\.\\d+)?)");
        private static final Pattern ACC_SUMMARY_TOTAL_VALUE_PATTERN = Pattern.compile(
                "Total\\s+(?<startValue>-?(US)?\\$(\\d+,)*\\d+(\\.\\d+)?)\\s+(?<endValue>-?(US)?\\$(\\d+,)*\\d+(\\.\\d+)?)");

        private static final DateTimeFormatter PERIOD_DF = DateTimeFormatter.ofPattern("dd MMM yyyy");

        private static final Pattern TRANSACTION_DATE_SYMBOL_TYPE_PATTERN = Pattern.compile("""
                (?<date>\\d{2}\\s+\\w{3}\\s+\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}\\s+[A-Z]{3})\
                (\\s*(?<symbol>.+))?\
                \\s+(?<type>(Custody fee)|(Dividend)|(Cash top-up)|(Cash withdrawal)|(Trade - Market)|(Trade - Limit)|(Stock split)|(Spinoff)|(Merger - stock))\
                \\s+(?<numbersPart>.*)"""
        );

        private static final Pattern VALUE_FEES_COMMISSION_PATTERN = Pattern.compile("""
                \\s*(?<value>-?(US)?\\$(\\d+,)*\\d+(\\.\\d+)?)\
                \\s*(?<fees>-?(US)?\\$(\\d+,)*\\d+(\\.\\d+)?)\
                \\s*(?<commission>-?(US)?\\$(\\d+,)*\\d+(\\.\\d+)?)"""
        );

        private static final Pattern QTY_VALUE_FEES_COMMISSIONS_PATTERN = Pattern.compile("""
                \\s*(?<quantity>-?(\\d+,)*\\d+(\\.\\d+)?)\
                \\s+(?<value>-?(US)?\\$(\\d+,)*\\d+(\\.\\d+)?)\
                \\s+(?<fees>-?(US)?\\$(\\d+,)*\\d+(\\.\\d+)?)\
                \\s+(?<commission>-?(US)?\\$(\\d+,)*\\d+(\\.\\d+)?)"""
        );

        private static final Pattern TRADE_PATTERN = Pattern.compile("""
                \\s*(?<quantity>-?(\\d+,)*\\d+(\\.\\d+)?)\
                \\s+(?<price>-?(US)?\\$?(\\d+,)*\\d+(\\.\\d+)?)\
                \\s*(?<side>(Buy)|(Sell))\
                \\s+(?<value>-?(US)?\\$(\\d+,)*\\d+(\\.\\d+)?)\
                \\s+(?<fees>-?(US)?\\$(\\d+,)*\\d+(\\.\\d+)?)\
                \\s+(?<commission>-?(US)?\\$(\\d+,)*\\d+(\\.\\d+)?)"""
        );

        private static final DateTimeFormatter TRANSACTION_DTF = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss O");
    }

    public TradingAccountStatement parseAccountStatement(List<String> lines) {
        int r = 0;
        int linesSize = lines.size();
        {
            boolean eurMainHeaderFonud = false;
            for (; r < linesSize; r++) {
                String line = stripToEmpty(lines.get(r));
                if (line.equals("Account Statement")) {
                    eurMainHeaderFonud = true;
                    r++;
                    break;
                }
            }
            Assert.isTrue(eurMainHeaderFonud);
        }
        {
            boolean eurSummaryFound = false;
            for (; r < linesSize; r++) {
                String line = stripToEmpty(lines.get(r));
                if (line.equals("EUR Account summary")) {
                    eurSummaryFound = true;
                    r++;
                    break;
                }
            }
            Assert.isTrue(eurSummaryFound);
        }
        {
            boolean eurBreakdownFound = false;
            for (; r < linesSize; r++) {
                String line = stripToEmpty(lines.get(r));
                if (line.equals("EUR Portfolio breakdown")) {
                    eurBreakdownFound = true;
                    r++;
                    break;
                }
            }
            Assert.isTrue(eurBreakdownFound);
        }
        {
            boolean eurTransactionsFound = false;
            for (; r < linesSize; r++) {
                String line = stripToEmpty(lines.get(r));
                if (line.equals("EUR Transactions")) {
                    eurTransactionsFound = true;
                    r++;
                    break;
                }
            }
            Assert.isTrue(eurTransactionsFound);
        }
        {
            boolean usdMainHeaderFonud = false;
            for (; r < linesSize; r++) {
                String line = stripToEmpty(lines.get(r));
                if (line.equals("Account Statement")) {
                    usdMainHeaderFonud = true;
                    r++;
                    break;
                }
            }
            Assert.isTrue(usdMainHeaderFonud);
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
            boolean usdSummaryFound = false;
            for (; r < linesSize; r++) {
                String line = stripToEmpty(lines.get(r));
                if (line.equals("USD Account summary")) {
                    usdSummaryFound = true;
                    r++;
                    break;
                }
            }
            Assert.isTrue(usdSummaryFound);
        }
        BigDecimal startStocksValue;
        BigDecimal startCashValue;
        BigDecimal startTotalValue;
        BigDecimal endStocksValue;
        BigDecimal endCashValue;
        BigDecimal endTotalValue;
        {
            String line = stripToEmpty(lines.get(r));
            Assert.isTrue(Lazy.ACC_SUMMARY_HEADER_PATTERN.matcher(line).find());
            r++;
            {
                line = lines.get(r++);
                Matcher matcher = Lazy.ACC_SUMMARY_STOCKS_VALUE_PATTERN.matcher(line);
                if (matcher.find()) {
                    startStocksValue = parseMoney(matcher.group("startValue"));
                    endStocksValue = parseMoney(matcher.group("endValue"));
                } else {
                    throw new IllegalStateException("Stocks value not found: " + line);
                }
            }
            {
                line = lines.get(r++);
                Matcher matcher = Lazy.ACC_SUMMARY_CASH_VALUE_PATTERN.matcher(line);
                if (matcher.find()) {
                    startCashValue = parseMoney(matcher.group("startValue"));
                    endCashValue = parseMoney(matcher.group("endValue"));
                } else {
                    throw new IllegalStateException("Cash value not found: " + line);
                }
            }
            {
                line = lines.get(r++);
                Matcher matcher = Lazy.ACC_SUMMARY_TOTAL_VALUE_PATTERN.matcher(line);
                if (matcher.find()) {
                    startTotalValue = parseMoney(matcher.group("startValue"));
                    endTotalValue = parseMoney(matcher.group("endValue"));
                } else {
                    throw new IllegalStateException("Total value not found: " + line);
                }
            }
        }
        {
            boolean usdTransactionsFound = false;
            for (; r < linesSize; r++) {
                String line = stripToEmpty(lines.get(r));
                if (line.equals("USD Transactions")) {
                    usdTransactionsFound = true;
                    r++;
                    break;
                }
            }
            Assert.isTrue(usdTransactionsFound);
        }
        List<Transaction> transactions = new ArrayList<>();
        {
            for (; r < linesSize; r++) {
                String line = stripToEmpty(lines.get(r));
                if (line.isBlank()) {
                    continue;
                }
                if (Lazy.TRANSACTIONS_HEADER_PATTERN.matcher(line).matches()) {
                    continue;
                }
                if (line.startsWith("This statement is provided by Revolut Securities Europe UAB")) {
                    r++;
                    break;
                }
                if (line.startsWith("Get help directly In app")) {
                    r++;
                    break;
                }
                if (line.startsWith("Report lost or stolen card")) {
                    r++;
                    break;
                }
                if (line.contains("Transfer from Revolut Bank UAB to Revolut Securities Europe UAB")) {
                    continue;
                }
                if (line.contains("Transfer from Revolut Trading Ltd to Revolut Securities Europe UAB")) {
                    continue;
                }
                Transaction transaction = parseTradingAccountTransactionLine(Currency.USD, "US", line);
                transactions.add(transaction);
            }
        }

        return new TradingAccountStatement(
                accountName,
                accountNumber,
                periodFrom,
                periodTo,
                Currency.USD,
                startStocksValue,
                startCashValue,
                startTotalValue,
                endStocksValue,
                endCashValue,
                endTotalValue,
                transactions
        );
    }

    private Transaction parseTradingAccountTransactionLine(Currency ccy, String marketCountry, String line) {

        Transaction.TransactionBuilder tranBuilder = Transaction.builder()
                .ccy(ccy)
                .country(marketCountry);

        TransactionType transactionType;
        String numbersPart;
        {
            Matcher matcher = Lazy.TRANSACTION_DATE_SYMBOL_TYPE_PATTERN.matcher(line);
            boolean matchFound = matcher.find();
            if (!matchFound) {
                throw new IllegalStateException(String.format("Could not parse transaction line: '%s'", line));
            }
            transactionType = parseTransactionType(matcher.group("type"));
            numbersPart = matcher.group("numbersPart");

            tranBuilder.date(ZonedDateTime.parse(matcher.group("date"), Lazy.TRANSACTION_DTF));
            tranBuilder.symbol(matcher.group("symbol"));
            tranBuilder.type(transactionType);
        }
        {
            Pattern pattern = switch (transactionType) {
                case CASH_TOP_UP, CASH_WITHDRAWAL, CUSTODY_FEE, DIVIDEND -> Lazy.VALUE_FEES_COMMISSION_PATTERN;
                case SPINOFF, STOCK_SPLIT, MERGER -> Lazy.QTY_VALUE_FEES_COMMISSIONS_PATTERN;
                case TRADE_LIMIT, TRADE_MARKET -> Lazy.TRADE_PATTERN;
            };

            Matcher matcher = pattern.matcher(numbersPart);
            boolean matchFound = matcher.find();
            if (!matchFound) {
                throw new IllegalStateException(String.format("Could not parse transaction line: '%s'", line));
            }

            tranBuilder.value(parseMoney(matcher.group("value")));
            tranBuilder.fees(parseMoney(matcher.group("fees")));
            tranBuilder.commission(parseMoney(matcher.group("commission")));
            if (pattern == Lazy.QTY_VALUE_FEES_COMMISSIONS_PATTERN) {
                tranBuilder.qty(parseDecimal(matcher.group("quantity")));
            } else if (pattern == Lazy.TRADE_PATTERN) {
                tranBuilder.qty(parseDecimal(matcher.group("quantity")));
                tranBuilder.price(parseMoney(matcher.group("price")));
                tranBuilder.side(TransactionSide.valueOf(matcher.group("side").toUpperCase()));
            }
        }
        return tranBuilder.build();
    }

    private TransactionType parseTransactionType(String transactionTypeStr) {
        return transactionTypeStr == null ? null : switch (transactionTypeStr) {
            case "Custody fee" -> TransactionType.CUSTODY_FEE;
            case "Dividend" -> TransactionType.DIVIDEND;
            case "Cash top-up" -> TransactionType.CASH_TOP_UP;
            case "Cash withdrawal" -> TransactionType.CASH_WITHDRAWAL;
            case "Trade - Market" -> TransactionType.TRADE_MARKET;
            case "Trade - Limit" -> TransactionType.TRADE_LIMIT;
            case "Stock split" -> TransactionType.STOCK_SPLIT;
            case "Spinoff" -> TransactionType.SPINOFF;
            case "Merger - stock" -> TransactionType.MERGER;
            case null, default -> throw new IllegalArgumentException("Unsupported transactionType: '%s'".formatted(transactionTypeStr));
        };
    }


}
