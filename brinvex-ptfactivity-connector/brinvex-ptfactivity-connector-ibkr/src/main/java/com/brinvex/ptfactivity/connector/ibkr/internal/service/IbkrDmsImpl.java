package com.brinvex.ptfactivity.connector.ibkr.internal.service;

import com.brinvex.ptfactivity.connector.ibkr.api.model.IbkrDocKey;
import com.brinvex.ptfactivity.connector.ibkr.api.model.IbkrDocKey.ActivityDocKey;
import com.brinvex.ptfactivity.connector.ibkr.api.model.IbkrDocKey.TradeConfirmDocKey;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrDms;
import com.brinvex.ptfactivity.core.api.exception.StorageException;
import com.brinvex.dms.api.Dms;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;
import java.util.SequencedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Comparator.naturalOrder;

@SuppressWarnings("unused")
public class IbkrDmsImpl implements IbkrDms {

    private final Dms dms;

    public IbkrDmsImpl(Dms dms) {
        this.dms = dms;
    }

    @Override
    public List<ActivityDocKey> getActivityDocKeys(String accountId, LocalDate fromDateIncl, LocalDate toDateIncl) {
        String directory = getDirectory(accountId);
        SequencedCollection<String> rawKeys = dms.getKeys(directory);
        List<ActivityDocKey> results = new ArrayList<>();
        for (String rawKey : rawKeys) {
            ActivityDocKey docKey = parseActDocKey(rawKey);
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

    private ActivityDocKey parseActDocKey(String fileKey) {
        ActivityDocKey docKey;
        Matcher m = Lazy.actDmsRawKeyPattern.matcher(fileKey);
        if (m.matches()) {
            String docAccountId = m.group(1);
            LocalDate docFromDateIncl = LocalDate.parse(m.group(2), Lazy.rawKeyDf);
            LocalDate docToDateIncl = LocalDate.parse(m.group(3), Lazy.rawKeyDf);
            docKey = new ActivityDocKey(docAccountId, docFromDateIncl, docToDateIncl);
        } else {
            docKey = null;
        }
        return docKey;
    }

    @Override
    public List<TradeConfirmDocKey> getTradeConfirmDocKeys(String accountId, LocalDate fromDateIncl, LocalDate toDateIncl) {
        String directory = getDirectory(accountId);
        SequencedCollection<String> rawKeys = dms.getKeys(directory);
        List<TradeConfirmDocKey> results = new ArrayList<>();
        Pattern p = Lazy.tcDmsRawKeyPattern;
        for (String rawKey : rawKeys) {
            Matcher m = p.matcher(rawKey);
            if (m.matches()) {
                String docAccountId = m.group(1);
                if (!docAccountId.equals(accountId)) {
                    throw new StorageException("Unexpected document '%s' in directory '%s'".formatted(rawKey, directory));
                }
                LocalDate docDate = LocalDate.parse(m.group(2), Lazy.rawKeyDf);
                boolean outside = (toDateIncl != null && docDate.isAfter(toDateIncl)) || (fromDateIncl != null && docDate.isBefore(fromDateIncl));
                if (outside) {
                    continue;
                }
                results.add(new TradeConfirmDocKey(docAccountId, docDate));
            }
        }
        results.sort(naturalOrder());
        return results;
    }

    @Override
    public String getStatementContent(IbkrDocKey docKey) {
        String directory = getDirectory(docKey.accountId());
        String fileKey = constructFileKey(docKey);
        return dms.getTextContent(directory, fileKey);
    }

    @Override
    public List<String> getStatementContentLines(IbkrDocKey docKey, int limit) {
        String directory = getDirectory(docKey.accountId());
        String fileKey = constructFileKey(docKey);
        return dms.getTextLines(directory, fileKey, limit);
    }

    @Override
    public boolean putActivityStatement(ActivityDocKey docKey, String content) {
        String accountId = docKey.accountId();
        String directory = getDirectory(accountId);

        List<ActivityDocKey> actDocKeys = new ArrayList<>(getActivityDocKeys(accountId, docKey.fromDateIncl(), docKey.toDateIncl()));
        actDocKeys.add(docKey);
        SequencedSet<ActivityDocKey> redundantActKeys = new LinkedHashSet<>(dms.getRedundantPeriodKeys(
                actDocKeys, ActivityDocKey::fromDateIncl, ActivityDocKey::toDateIncl));

        boolean newSaved;
        if (!redundantActKeys.remove(docKey)) {
            String newFileKey = constructFileKey(docKey);
            dms.add(directory, newFileKey, content);
            newSaved = true;
        } else {
            newSaved = false;
        }
        dms.delete(directory, redundantActKeys.stream().map(this::constructFileKey).toList());

        return newSaved;
    }

    @Override
    public boolean putTradeConfirmStatement(TradeConfirmDocKey docKey, String content) {
        String accountId = docKey.accountId();
        String directory = getDirectory(accountId);
        String newFileKey = constructFileKey(docKey);
        return dms.put(directory, newFileKey, content);
    }

    @Override
    public void delete(IbkrDocKey docKey) {
        String directory = getDirectory(docKey.accountId());
        String fileKey = constructFileKey(docKey);
        dms.delete(directory, fileKey);
    }

    private String constructFileKey(IbkrDocKey docKey) {
        return switch (docKey) {
            case ActivityDocKey actDocKey -> "%s-ACT-%s-%s.xml".formatted(
                    actDocKey.accountId(),
                    Lazy.rawKeyDf.format(actDocKey.fromDateIncl()),
                    Lazy.rawKeyDf.format(actDocKey.toDateIncl())
            );
            case TradeConfirmDocKey tcDocKey -> "%s-TC-%s.xml".formatted(
                    tcDocKey.accountId(),
                    Lazy.rawKeyDf.format(tcDocKey.date())
            );
        };
    }

    private String getDirectory(String accountId) {
        return accountId;
    }

    private static final class Lazy {
        private static final DateTimeFormatter rawKeyDf = DateTimeFormatter.ofPattern("yyyyMMdd");
        private static final Pattern actDmsRawKeyPattern = Pattern.compile("(\\w{8,16})-ACT-(\\d{8})-(\\d{8})\\.xml");
        private static final Pattern tcDmsRawKeyPattern = Pattern.compile("(\\w{8,16})-TC-(\\d{8})\\.xml");
    }

}
