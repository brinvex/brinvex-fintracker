package com.brinvex.ptfactivity.connector.fiob.internal.service.parser;

import com.brinvex.finance.types.vo.Money;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.Lang;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.Statement;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.TradingTransaction;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.TradingTransaction.TradingTransactionBuilder;
import com.brinvex.java.validation.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toCollection;

public class FiobTradingStatementParser {

    @SuppressWarnings("SpellCheckingInspection")
    public Statement.TradingTransStatement parseTransactionStatement(String transStatementContent) {
        List<String> lines = transStatementContent
                .lines()
                .collect(toCollection(ArrayList::new));

        String accountNumber;
        Lang lang;
        LocalDate periodFrom;
        LocalDate periodTo;
        {
            String line1 = lines.removeFirst();
            Matcher matcher = Lazy.ACCOUNT_NUMBER_PATTERN.matcher(line1);
            if (!matcher.find()) {
                throw new IllegalStateException("Could not parse account number: '%s'".formatted(line1));
            }
            accountNumber = matcher.group("accountNumber");
            if (line1.startsWith("Overview")) {
                lang = Lang.EN;
            } else if (line1.startsWith("Přehled")) {
                lang = Lang.CZ;
            } else if (line1.startsWith("Prehľad")) {
                lang = Lang.SK;
            } else {
                throw new IllegalStateException("Could not detect lang: '%s'".formatted(line1));
            }
        }
        {
            //E.g. Vytvorené: 15.01.2023 22:28:06
            lines.removeFirst();
        }
        {
            String line3 = lines.removeFirst();
            Matcher matcher = Lazy.PERIOD_PATTERN.matcher(line3);
            if (!matcher.find()) {
                throw new IllegalStateException("Could not parse period: '%s'".formatted(line3));
            }
            periodFrom = LocalDate.parse(matcher.group("periodFrom"), Lazy.PERIOD_DF);
            periodTo = LocalDate.parse(matcher.group("periodTo"), Lazy.PERIOD_DF);
        }

        lines = lines.stream().dropWhile(String::isBlank).takeWhile(not(String::isBlank)).collect(toCollection(ArrayList::new));

        Map<TradingTransColumnParser, Integer> columnParsers = new LinkedHashMap<>();
        {
            String headersLine = lines.removeFirst();
            String[] headerTitles = Lazy.COLUMN_DELIMITER_PATTERN.split(headersLine, -1);
            for (int j = 0, headerTitlesLength = headerTitles.length; j < headerTitlesLength; j++) {
                String headerTitle = headerTitles[j];
                TradingTransColumnParser header = TradingTransColumnParser.ofTitle(headerTitle, lang);
                if (header == null) {
                    continue;
                }
                columnParsers.put(header, j);
            }
            Set<TradingTransColumnParser> missingHeaders = new LinkedHashSet<>(TradingTransColumnParser.STANDARD_COLUMNS);
            missingHeaders.removeAll(columnParsers.keySet());
            if (!missingHeaders.isEmpty()) {
                throw new IllegalStateException("Mising mandatory headers: %s, line='%s'".formatted(missingHeaders, headersLine));
            }
        }

        {
            String lastLine = lines.removeLast();
            //E.g. ;;Súčet;;;;
            Assert.isTrue(lastLine.startsWith(";"));
        }

        lines = lines.reversed();

        List<TradingTransaction> rawTrans = new ArrayList<>();
        Map<String, Integer> linesDuplicatesCount = new LinkedHashMap<>();
        for (String line : lines) {
            try {
                TradingTransactionBuilder rawTranBuilder = TradingTransaction.builder();
                {
                    List<String> cells = List.of(Lazy.COLUMN_DELIMITER_PATTERN.split(line, -1));
                    for (Map.Entry<TradingTransColumnParser, Integer> e : columnParsers.entrySet()) {
                        TradingTransColumnParser columnParser = e.getKey();
                        Integer cellIdx = e.getValue();
                        String cell = cells.get(cellIdx);
                        cell = cell.trim();
                        columnParser.fill(rawTranBuilder, cell, lang);
                    }
                }

                rawTranBuilder.rowNumberOverStatementLine(linesDuplicatesCount.merge(line, 1, Integer::sum));
                rawTranBuilder.statementLineHash(line.hashCode());
                TradingTransaction tran = rawTranBuilder.build();
                rawTrans.add(tran);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to parse: '%s'".formatted(line), e);
            }
        }

        return new Statement.TradingTransStatement(
                accountNumber,
                periodFrom,
                periodTo,
                rawTrans,
                lang
        );
    }

    /*
    E.g.
    Vytvorené: 15.01.2023 22:28:06
    Vytvořeno: 14.11.2024 12:43:27
    Created: 14.11.2024 12:38:03
     */
    @SuppressWarnings("SpellCheckingInspection")
    public LocalDateTime parseTradingTransStatementCreatedOn(List<String> statementHeaderLines) {
        String prefixSK = "Vytvorené: ";
        String prefixCZ = "Vytvořeno: ";
        String prefixEN = "Created: ";
        int prefixLengthSK = prefixSK.length();
        int prefixLengthCZ = prefixCZ.length();
        int prefixLengthEN = prefixEN.length();
        for (String headerLine : statementHeaderLines) {
            if (headerLine.startsWith(prefixSK)) {
                headerLine = headerLine.substring(prefixLengthSK);
            } else if (headerLine.startsWith(prefixCZ)) {
                headerLine = headerLine.substring(prefixLengthCZ);
            } else if (headerLine.startsWith(prefixEN)) {
                headerLine = headerLine.substring(prefixLengthEN);
            } else {
                continue;
            }
            return LocalDateTime.parse(headerLine, Lazy.CREATED_ON_DTF);
        }
        throw new IllegalArgumentException("CreatedOn header line not found");
    }

    public Statement.TradingSnapshotStatement parsePtfSnapshotStatement(String ptfStatementContent) {
        List<String> lines = ptfStatementContent.lines().collect(Collectors.toList());
        String line3 = lines.get(2);
        Matcher matcher = Lazy.PERIOD_PATTERN.matcher(line3);
        if (!matcher.find()) {
            String message = String.format("Could not parse period: '%s'", line3);
            throw new IllegalStateException(message);
        }
        LocalDate periodTo = LocalDate.parse(matcher.group("periodTo"), Lazy.PERIOD_DF);

        Collections.reverse(lines);
        String lastLine = lines
                .stream()
                .dropWhile(String::isBlank)
                .findFirst()
                .orElseThrow();
        String[] parts = lastLine.split(";");
        String ccy = parts[0].substring(parts[0].length() - 4, parts[0].length() - 1);
        BigDecimal totalValue = new BigDecimal(parts[10].replace(',', '.').replace(" ", ""));

        return new Statement.TradingSnapshotStatement(periodTo, new Money(ccy, totalValue));
    }

    private static class Lazy {
        static final Pattern COLUMN_DELIMITER_PATTERN = Pattern.compile(";");
        static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile(".*:\\s+(?<accountNumber>\\d+)\"");
        static final Pattern PERIOD_PATTERN = Pattern.compile(".*:\\s+(?<periodFrom>\\d{1,2}\\.\\d{1,2}\\.\\d{4})\\s+-\\s+(?<periodTo>\\d{1,2}\\.\\d{1,2}\\.\\d{4})");

        static final DateTimeFormatter PERIOD_DF = DateTimeFormatter.ofPattern("d.M.yyyy");
        static final DateTimeFormatter CREATED_ON_DTF = DateTimeFormatter.ofPattern("d.M.yyyy HH:mm:ss");
    }
}
