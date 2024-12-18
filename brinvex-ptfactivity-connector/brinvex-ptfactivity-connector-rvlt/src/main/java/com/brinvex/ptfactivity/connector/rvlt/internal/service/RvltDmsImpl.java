package com.brinvex.ptfactivity.connector.rvlt.internal.service;

import com.brinvex.ptfactivity.connector.rvlt.api.model.RvltDocKey.TradingAccountStatementDocKey;
import com.brinvex.ptfactivity.connector.rvlt.api.model.RvltDocKey.PnlStatementDocKey;
import com.brinvex.dms.api.Dms;
import com.brinvex.ptfactivity.connector.rvlt.api.service.RvltDms;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.SequencedCollection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Comparator.naturalOrder;

@SuppressWarnings("unused")
public class RvltDmsImpl implements RvltDms {

    private final Dms dms;

    public RvltDmsImpl(Dms dms) {
        this.dms = dms;
    }

    @Override
    public List<TradingAccountStatementDocKey> getTradingAccountStatementDocKeys(String accountNumber, LocalDate fromDateIncl, LocalDate toDateIncl) {
        String directory = getTradingAccStatementDirectory(accountNumber);
        SequencedCollection<String> rawKeys = dms.getKeys(directory);
        List<TradingAccountStatementDocKey> results = new ArrayList<>();
        for (String rawKey : rawKeys) {
            TradingAccountStatementDocKey docKey = parseAccountStatementDocKey(accountNumber, rawKey);
            if (docKey != null) {
                boolean outside = (toDateIncl != null && docKey.fromDateIncl().isAfter(toDateIncl)) || (fromDateIncl != null && docKey.toDateIncl().isBefore(fromDateIncl));
                if (outside) {
                    continue;
                }
                results.add(docKey);
            }
        }
        results.sort(naturalOrder());
        return results;
    }

    @Override
    public byte[] getStatementContent(TradingAccountStatementDocKey docKey) {
        String accountNumber = docKey.accountNumber();
        String directory = getTradingAccStatementDirectory(accountNumber);
        SequencedCollection<String> rawKeys = dms.getKeys(directory);
        String rawKey = rawKeys.stream()
                .filter(_rawKey -> docKey.equals(parseAccountStatementDocKey(accountNumber, _rawKey)))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("docKey not found: " + docKey));
        return dms.getBinaryContent(directory, rawKey);
    }

    @Override
    public List<PnlStatementDocKey> getPnlStatementDocKeys(String accountNumber, LocalDate fromDateIncl, LocalDate toDateIncl) {
        String directory = getPnlStatementDirectory(accountNumber);
        SequencedCollection<String> rawKeys = dms.getKeys(directory);
        List<PnlStatementDocKey> results = new ArrayList<>();
        for (String rawKey : rawKeys) {
            PnlStatementDocKey docKey = parsePnlStatementDocKey(accountNumber, rawKey);
            if (docKey != null) {
                boolean outside = (toDateIncl != null && docKey.fromDateIncl().isAfter(toDateIncl)) || (fromDateIncl != null && docKey.toDateIncl().isBefore(fromDateIncl));
                if (outside) {
                    continue;
                }
                results.add(docKey);
            }
        }
        results.sort(naturalOrder());
        return results;
    }

    @Override
    public byte[] getStatementContent(PnlStatementDocKey docKey) {
        String accountNumber = docKey.accountNumber();
        String directory = getPnlStatementDirectory(accountNumber);
        SequencedCollection<String> rawKeys = dms.getKeys(directory);
        String rawKey = rawKeys.stream()
                .filter(_rawKey -> docKey.equals(parsePnlStatementDocKey(accountNumber, _rawKey)))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("docKey not found: " + docKey));
        return dms.getBinaryContent(directory, rawKey);
    }

    private TradingAccountStatementDocKey parseAccountStatementDocKey(String accountNumber, String rawKey) {
        TradingAccountStatementDocKey docKey;
        Matcher m = Lazy.tradingAccountStatementRawKeyPattern.matcher(rawKey);
        if (m.matches()) {
            LocalDate docFromDateIncl = LocalDate.parse(m.group(1));
            LocalDate docToDateIncl = LocalDate.parse(m.group(2));
            docKey = new TradingAccountStatementDocKey(accountNumber, docFromDateIncl, docToDateIncl);
        } else {
            docKey = null;
        }
        return docKey;
    }

    private PnlStatementDocKey parsePnlStatementDocKey(String accountNumber, String rawKey) {
        PnlStatementDocKey docKey;
        Matcher m = Lazy.pnlStatementRawKeyPattern.matcher(rawKey);
        if (m.matches()) {
            LocalDate docFromDateIncl = LocalDate.parse(m.group(1));
            LocalDate docToDateIncl = LocalDate.parse(m.group(2));
            docKey = new PnlStatementDocKey(accountNumber, docFromDateIncl, docToDateIncl);
        } else {
            docKey = null;
        }
        return docKey;
    }

    private String getTradingAccStatementDirectory(String accountNumber) {
        return accountNumber + "/trading-account-statement";
    }

    private String getPnlStatementDirectory(String accountNumber) {
        return accountNumber;
    }

    private static final class Lazy {
        //E.g. trading-account-statement_2020-07-01_2020-08-01_en-us_5f4277
        private static final Pattern tradingAccountStatementRawKeyPattern = Pattern.compile("trading-account-statement_(\\d{4}-\\d{2}-\\d{2})_(\\d{4}-\\d{2}-\\d{2})_.{12}\\.pdf");

        //E.g. trading-pnl-statement_2020-02-01_2024-10-28_en-us_aae243.pdf
        private static final Pattern pnlStatementRawKeyPattern = Pattern.compile("trading-pnl-statement_(\\d{4}-\\d{2}-\\d{2})_(\\d{4}-\\d{2}-\\d{2})_.{12}\\.pdf");
    }


}
