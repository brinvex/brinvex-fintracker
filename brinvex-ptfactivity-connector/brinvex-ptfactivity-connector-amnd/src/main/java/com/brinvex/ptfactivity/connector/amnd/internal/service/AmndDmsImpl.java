package com.brinvex.ptfactivity.connector.amnd.internal.service;

import com.brinvex.ptfactivity.connector.amnd.api.model.AmndTransStatementDocKey;
import com.brinvex.ptfactivity.connector.amnd.api.service.AmndDms;
import com.brinvex.dms.api.Dms;

import java.time.LocalDate;
import java.util.SequencedCollection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.brinvex.java.collection.CollectionUtil.getFirstThrowIfMore;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;

public class AmndDmsImpl implements AmndDms {

    private final Dms dms;

    public AmndDmsImpl(Dms dms) {
        this.dms = dms;
    }

    @Override
    public AmndTransStatementDocKey getTradingAccountStatementDocKey(String accountId) {
        String directory = getStatementDirectory(accountId);
        String rawKey = getFirstThrowIfMore(dms.getKeys(directory));
        return parseAccountStatementDocKey(accountId, rawKey);
    }

    @Override
    public byte[] getStatementContent(AmndTransStatementDocKey docKey) {
        String accountId = docKey.accountId();
        String directory = getStatementDirectory(accountId);
        SequencedCollection<String> rawKeys = dms.getKeys(directory);
        String rawKey = rawKeys.stream()
                .filter(_rawKey -> docKey.equals(parseAccountStatementDocKey(accountId, _rawKey)))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("docKey not found: " + docKey));
        return dms.getBinaryContent(directory, rawKey);
    }

    private String getStatementDirectory(String accountId) {
        return accountId;
    }

    private AmndTransStatementDocKey parseAccountStatementDocKey(String accountId, String rawKey) {
        AmndTransStatementDocKey docKey;
        Matcher m = Lazy.transStatementRawKeyPattern.matcher(rawKey);
        if (m.matches()) {
            LocalDate docFromDateIncl = LocalDate.parse(m.group(1), BASIC_ISO_DATE);
            LocalDate docToDateIncl = LocalDate.parse(m.group(2), BASIC_ISO_DATE);
            docKey = new AmndTransStatementDocKey(accountId, docFromDateIncl, docToDateIncl);
        } else {
            docKey = null;
        }
        return docKey;
    }

    private static final class Lazy {
        private static final Pattern transStatementRawKeyPattern = Pattern.compile("Transakcny vypis.*-(\\d{8})-(\\d{8})\\.pdf");
    }


}
