package com.brinvex.ptfactivity.connector.fiob.internal.service;

import com.brinvex.ptfactivity.connector.fiob.api.model.FiobDocKey;
import com.brinvex.ptfactivity.connector.fiob.api.model.FiobDocKey.SavingTransDocKey;
import com.brinvex.ptfactivity.connector.fiob.api.model.FiobDocKey.TradingSnapshotDocKey;
import com.brinvex.ptfactivity.connector.fiob.api.model.FiobDocKey.TradingTransDocKey;
import com.brinvex.ptfactivity.connector.fiob.api.service.FiobDms;
import com.brinvex.ptfactivity.core.api.exception.StorageException;
import com.brinvex.dms.api.Dms;
import com.brinvex.java.validation.Assert;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;
import java.util.SequencedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static java.util.Comparator.naturalOrder;

@SuppressWarnings("DuplicatedCode")
public class FiobDmsImpl implements FiobDms {

    private final Dms dms;

    public FiobDmsImpl(Dms dms) {
        this.dms = dms;
    }

    @Override
    public List<TradingTransDocKey> getTradingTransDocKeys(String accountId, LocalDate fromDateIncl, LocalDate toDateIncl) {
        String directory = getDirectory(accountId);
        SequencedCollection<String> rawKeys = dms.getKeys(directory);
        List<TradingTransDocKey> results = new ArrayList<>();
        for (String rawKey : rawKeys) {
            TradingTransDocKey docKey = parseTradingTransactionsDocKey(rawKey);
            if (docKey != null) {
                if (!accountId.equals(docKey.accountId())) {
                    throw new StorageException("Unexpected document '%s' in directory '%s'".formatted(rawKey, directory));
                }
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
    public List<TradingSnapshotDocKey> getTradingSnapshotDocKeys(String accountId, LocalDate fromDateIncl, LocalDate toDateIncl) {
        String directory = getDirectory(accountId);
        SequencedCollection<String> rawKeys = dms.getKeys(directory);
        List<TradingSnapshotDocKey> results = new ArrayList<>();
        for (String rawKey : rawKeys) {
            TradingSnapshotDocKey docKey = parseTradingSnapshotDocKey(rawKey);
            if (docKey != null) {
                if (!accountId.equals(docKey.accountId())) {
                    throw new StorageException("Unexpected document '%s' in directory '%s'".formatted(rawKey, directory));
                }
                boolean outside = (toDateIncl != null && docKey.date().isAfter(toDateIncl)) || (fromDateIncl != null && docKey.date().isBefore(fromDateIncl));
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
    public List<SavingTransDocKey> getSavingTransDocKeys(String accountId, LocalDate fromDateIncl, LocalDate toDateIncl) {
        String directory = getDirectory(accountId);
        SequencedCollection<String> rawKeys = dms.getKeys(directory);
        List<SavingTransDocKey> results = new ArrayList<>();
        for (String rawKey : rawKeys) {
            SavingTransDocKey docKey = parseSavingTransactionsDocKey(rawKey);
            if (docKey != null) {
                if (!accountId.equals(docKey.accountId())) {
                    throw new StorageException("Unexpected document '%s' in directory '%s'".formatted(rawKey, directory));
                }
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

    private TradingTransDocKey parseTradingTransactionsDocKey(String fileKey) {
        Matcher m = Lazy.TRADING_TRANS_RAW_KEY_PATTERN.matcher(fileKey);
        if (m.matches()) {
            String docAccountId = m.group(1);
            LocalDate docFromDateIncl = LocalDate.parse(m.group(2), BASIC_ISO_DATE);
            LocalDate docToDateIncl = LocalDate.parse(m.group(3), BASIC_ISO_DATE);
            return new TradingTransDocKey(docAccountId, docFromDateIncl, docToDateIncl);
        }
        return null;
    }

    private TradingSnapshotDocKey parseTradingSnapshotDocKey(String fileKey) {
        Matcher m = Lazy.SNAPSHOT_RAW_KEY_PATTERN.matcher(fileKey);
        if (m.matches()) {
            String docAccountId = m.group(1);
            LocalDate docDate = LocalDate.parse(m.group(2), BASIC_ISO_DATE);
            return new TradingSnapshotDocKey(docAccountId, docDate);
        }
        return null;
    }

    private SavingTransDocKey parseSavingTransactionsDocKey(String fileKey) {
        Matcher m = Lazy.SAVING_TRANS_RAW_KEY_PATTERN.matcher(fileKey);
        if (m.matches()) {
            String docAccountId = m.group(1);
            LocalDate docFromDateIncl = LocalDate.parse(m.group(2), BASIC_ISO_DATE);
            LocalDate docToDateIncl = LocalDate.parse(m.group(3), BASIC_ISO_DATE);
            return new SavingTransDocKey(docAccountId, docFromDateIncl, docToDateIncl);
        }
        return null;
    }


    @Override
    public String getStatementContent(FiobDocKey docKey) {
        String directory = getDirectory(docKey.accountId());
        String fileKey = constructFileKey(docKey);
        return switch (docKey) {
            case SavingTransDocKey _ -> dms.getTextContent(directory, fileKey, UTF_8);
            case TradingSnapshotDocKey _, TradingTransDocKey _ -> dms.getTextContent(directory, fileKey, UTF_8, Lazy.FIOB_CHARSET);
        };
    }

    @Override
    public List<String> getStatementContentLinesIfExists(FiobDocKey docKey, int limit) {
        String directory = getDirectory(docKey.accountId());
        String fileKey = constructFileKey(docKey);
        if (!dms.exists(directory, fileKey)) {
            return null;
        }
        return switch (docKey) {
            case SavingTransDocKey _ -> dms.getTextLines(directory, fileKey, limit, UTF_8);
            case TradingSnapshotDocKey _, TradingTransDocKey _ -> dms.getTextLines(directory, fileKey, limit, UTF_8, Lazy.FIOB_CHARSET);
        };
    }

    @Override
    public LocalDateTime getStatementLastModifiedTimeIfExists(FiobDocKey docKey) {
        String directory = getDirectory(docKey.accountId());
        String fileKey = constructFileKey(docKey);
        if (!dms.exists(directory, fileKey)) {
            return null;
        }
        return dms.getLastModifiedTime(directory, fileKey);
    }

    @Override
    public boolean putStatement(FiobDocKey docKey, String content) {
        return switch (docKey) {
            case TradingSnapshotDocKey tradingSnapshotDocKey -> putTradingSnapshotStatement(tradingSnapshotDocKey, content);
            case TradingTransDocKey tradingTransDocKey -> putTradingTransStatement(tradingTransDocKey, content);
            case SavingTransDocKey savingTransDocKey -> putSavingTransStatement(savingTransDocKey, content);
        };
    }

    private boolean putTradingTransStatement(TradingTransDocKey docKey, String content) {
        String accountId = docKey.accountId();
        String directory = getDirectory(accountId);

        List<TradingTransDocKey> tradingTransDocKeys = new ArrayList<>(getTradingTransDocKeys(accountId, docKey.fromDateIncl(), docKey.toDateIncl()));
        tradingTransDocKeys.add(docKey);
        SequencedSet<TradingTransDocKey> redundantKeys = new LinkedHashSet<>(dms.getRedundantPeriodKeys(
                tradingTransDocKeys, TradingTransDocKey::fromDateIncl, TradingTransDocKey::toDateIncl));

        boolean newSaved;
        if (!redundantKeys.remove(docKey)) {
            String newFileKey = constructFileKey(docKey);
            Assert.notNull(parseTradingTransactionsDocKey(newFileKey));
            dms.add(directory, newFileKey, content);
            newSaved = true;
        } else {
            newSaved = false;
        }
        dms.delete(directory, redundantKeys.stream().map(this::constructFileKey).toList());
        return newSaved;
    }

    private boolean putTradingSnapshotStatement(TradingSnapshotDocKey docKey, String content) {
        String accountId = docKey.accountId();
        String directory = getDirectory(accountId);
        dms.put(directory, constructFileKey(docKey), content);
        return true;
    }

    private boolean putSavingTransStatement(SavingTransDocKey docKey, String content) {
        String accountId = docKey.accountId();
        String directory = getDirectory(accountId);

        SequencedSet<SavingTransDocKey> savingTransDocKeys = new LinkedHashSet<>(getSavingTransDocKeys(accountId, docKey.fromDateIncl(), docKey.toDateIncl()));
        savingTransDocKeys.add(docKey);
        SequencedSet<SavingTransDocKey> redundantKeys = new LinkedHashSet<>(dms.getRedundantPeriodKeys(
                savingTransDocKeys, SavingTransDocKey::fromDateIncl, SavingTransDocKey::toDateIncl));

        boolean newSaved;
        if (!redundantKeys.remove(docKey)) {
            String newFileKey = constructFileKey(docKey);
            Assert.notNull(parseSavingTransactionsDocKey(newFileKey));
            dms.put(directory, newFileKey, content);
            newSaved = true;
        } else {
            newSaved = false;
        }
        dms.delete(directory, redundantKeys.stream().map(this::constructFileKey).toList());
        return newSaved;
    }


    @Override
    public void delete(FiobDocKey docKey) {
        String directory = getDirectory(docKey.accountId());
        String fileKey = constructFileKey(docKey);
        dms.delete(directory, fileKey);
    }

    private String constructFileKey(FiobDocKey docKey) {
        return switch (docKey) {
            case TradingTransDocKey tradingTransDocKey -> "%s-Transactions-%s-%s.csv".formatted(
                    tradingTransDocKey.accountId(),
                    BASIC_ISO_DATE.format(tradingTransDocKey.fromDateIncl()),
                    BASIC_ISO_DATE.format(tradingTransDocKey.toDateIncl())
            );
            case TradingSnapshotDocKey tradingSnapshotDocKey -> "%s-Snapshot-%s.csv".formatted(
                    docKey.accountId(),
                    BASIC_ISO_DATE.format(tradingSnapshotDocKey.date())
            );
            case SavingTransDocKey savingTransDocKey -> "%s-Transactions-%s-%s.xml".formatted(
                    savingTransDocKey.accountId(),
                    BASIC_ISO_DATE.format(savingTransDocKey.fromDateIncl()),
                    BASIC_ISO_DATE.format(savingTransDocKey.toDateIncl())
            );
        };
    }

    private String getDirectory(String accountId) {
        return accountId;
    }

    private static final class Lazy {
        private static final Charset FIOB_CHARSET = Charset.forName("windows-1250");
        private static final Pattern TRADING_TRANS_RAW_KEY_PATTERN = Pattern.compile("(\\w{8,20})-Transactions-(\\d{8})-(\\d{8})\\.csv");
        private static final Pattern SNAPSHOT_RAW_KEY_PATTERN = Pattern.compile("(\\w{8,20})-Snapshot-(\\d{8})\\.csv");
        private static final Pattern SAVING_TRANS_RAW_KEY_PATTERN = Pattern.compile("(\\w{8,20})-Transactions-(\\d{8})-(\\d{8})\\.xml");
    }

}
