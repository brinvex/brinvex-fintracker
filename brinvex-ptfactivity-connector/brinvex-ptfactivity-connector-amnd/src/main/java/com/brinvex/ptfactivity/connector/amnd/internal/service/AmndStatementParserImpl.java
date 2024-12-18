package com.brinvex.ptfactivity.connector.amnd.internal.service;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.connector.amnd.api.model.statement.Trade;
import com.brinvex.ptfactivity.connector.amnd.api.model.statement.TradeType;
import com.brinvex.ptfactivity.connector.amnd.api.model.statement.TransactionStatement;
import com.brinvex.ptfactivity.connector.amnd.api.service.AmndStatementParser;
import com.brinvex.ptfactivity.core.api.facade.PdfReaderFacade;
import com.brinvex.java.StringUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.util.function.Predicate.not;

public class AmndStatementParserImpl implements AmndStatementParser {

    private final PdfReaderFacade pdfReader;

    public AmndStatementParserImpl(PdfReaderFacade pdfReader) {
        this.pdfReader = pdfReader;
    }

    @SuppressWarnings({"SpellCheckingInspection", "unused", "UnusedAssignment"})
    @Override
    public TransactionStatement parseTrades(byte[] statementPdfContent) {
        List<String> lines = pdfReader.readPdfLines(statementPdfContent);

        String accountId = lines.get(1).trim();
        assertLine(2, accountId, Lazy.ACCOUNT_ID_PATTERN);

        int tranIdx = (int) (
                lines.stream()
                        .skip(2)
                        .map(String::trim)
                        .takeWhile(not("Výpis operácií z registra podielnikov"::equals))
                        .count() + 3);

        String line = lines.get(tranIdx);
        assertLine(tranIdx, line, "Dátum a čas");
        line = lines.get(++tranIdx);
        assertLine(tranIdx, line, "zobchodovania");
        line = lines.get(++tranIdx);
        assertLine(tranIdx, line, "Dátum a čas");
        line = lines.get(++tranIdx);
        assertLine(tranIdx, line, "vysporiadani");
        line = lines.get(++tranIdx);
        assertLine(tranIdx, line, "a");
        line = lines.get(++tranIdx);
        assertLine(tranIdx, line, "Dátum a čas");
        line = lines.get(++tranIdx);
        assertLine(tranIdx, line, "pokynu");

        lines = lines.stream().filter(s -> s.length() < 120).toList();

        List<Trade> trades = new ArrayList<>();
        for (int i = tranIdx + 1; i < lines.size(); i++) {

            TradeType tradeType;
            LocalDate orderDate;
            LocalTime tradeTime;
            LocalDate settleDate;
            BigDecimal fee;
            BigDecimal netAmount1;
            BigDecimal netAmount2;
            LocalDate tradeDate;
            BigDecimal qty;
            BigDecimal unitPrice;
            LocalDate priceDate;
            String instName;
            String isin;
            String desc1;
            String desc2;

            try {
                desc1 = lines.get(i);
                desc2 = lines.get(++i);
                if (desc1.startsWith("Investícia")) {
                    line = lines.get(++i);
                    assertLine(i, line, "Bezhotovostný prevod");
                    tradeType = TradeType.BUY;
                } else if (desc1.startsWith("Spätné odkúpenie")) {
                    tradeType = TradeType.SELL;
                } else {
                    break;
                }

                {
                    String[] parts = lines.get(++i).trim().split("EUR");
                    assertLine(i, parts[0].trim(), Lazy.MONEY_PATTERN);
                    fee = parseDecimal(parts[0]);
                    netAmount1 = parseDecimal(parts[1]);
                    netAmount2 = parseDecimal(parts[2]);
                    if (netAmount1.compareTo(BigDecimal.ZERO) != 0
                        && netAmount1.setScale(0, RoundingMode.DOWN).compareTo(netAmount2.setScale(0, RoundingMode.DOWN)) != 0) {
                        String errMsg = "Unexpected %s.line: %s %s".formatted(i + 1, netAmount1, netAmount2);
                        throw new IllegalStateException(errMsg);
                    }
                    tradeDate = LocalDate.parse(parts[3].trim(), Lazy.DF);
                }

                line = lines.get(++i);
                tradeTime = LocalTime.parse(line);

                line = lines.get(++i);
                settleDate = LocalDate.parse(line, Lazy.DF);

                line = lines.get(++i);
                assertLine(i, line, "00:00:00");

                line = lines.get(++i);
                orderDate = LocalDate.parse(line, Lazy.DF);

                line = lines.get(++i);

                {
                    line = lines.get(++i);
                    int n1 = line.indexOf(' ');
                    String part0 = line.substring(0, n1);
                    unitPrice = parseDecimal(part0);

                    String part1 = line.substring(n1 + 1, n1 + 4);
                    assertLine(i, part1, "EUR");

                    int n2 = line.lastIndexOf(' ');
                    qty = parseDecimal(line.substring(n2));

                    String innerStr = line.substring(n1 + 4, n2);
                    int n3 = innerStr.lastIndexOf(' ');
                    instName = innerStr.substring(0, n3);
                    priceDate = LocalDate.parse(innerStr.substring(n3 + 1, n3 + 11), Lazy.DF);
                }

                isin = lines.get(++i);
                assertLine(i, isin, Lazy.ISIN_PATTERN);

            } catch (Exception e) {
                throw new IllegalStateException("Parsing failed - %s".formatted(line), e);
            }

            BigDecimal tranNetAmount = netAmount2.negate();
            trades.add(Trade.builder()
                    .accountId(accountId)
                    .type(tradeType)
                    .orderDate(orderDate)
                    .tradeDate(tradeDate)
                    .settleDate(settleDate)
                    .isin(isin)
                    .instrumentName(instName)
                    .description("%s %s".formatted(desc1, desc2).trim())
                    .ccy(Currency.EUR)
                    .fee(fee.negate())
                    .netValue(tranNetAmount)
                    .qty(qty)
                    .price(unitPrice)
                    .priceDate(priceDate)
                    .build());
        }
        return new TransactionStatement(accountId, trades);
    }

    private void assertLine(int lineIdxZeroBased, String actual, String expected) {
        if (!Objects.equals(actual, expected)) {
            String errMsg = "Unexpected %s.line: '%s'".formatted(lineIdxZeroBased + 1, actual);
            throw new IllegalStateException(errMsg);
        }
    }

    private void assertLine(int lineIdxZeroBased, String actual, Pattern expectedPattern) {
        if (actual == null || !expectedPattern.matcher(actual).matches()) {
            String errMsg = "Unexpected %s.line: '%s'".formatted(lineIdxZeroBased + 1, actual);
            throw new IllegalStateException(errMsg);
        }
    }

    private static BigDecimal parseDecimal(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        s = StringUtil.deleteAllWhitespaces(s);
        s = s.replace(',', '.');
        return new BigDecimal(s);
    }

    private static class Lazy {
        static final Pattern ACCOUNT_ID_PATTERN = Pattern.compile("\\d{8,}");
        static final Pattern MONEY_PATTERN = Pattern.compile("-?(\\d+\\s)*\\d+,\\d{2}");
        static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        static final Pattern ISIN_PATTERN = Pattern.compile("([A-Z]{2}[A-Z\\d]{9}\\d)");

    }
}
