package com.brinvex.fintracker.connector.ibkr.impl.service;

import com.brinvex.fintracker.connector.ibkr.api.model.IbkrDocKey;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrDocKey.ActivityDocKey;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrDocKey.TradeConfirmDocKey;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrDms;
import com.brinvex.fintracker.api.exception.StorageException;
import com.brinvex.util.dms.api.Dms;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;

@SuppressWarnings("unused")
public class IbkrDmsImpl implements IbkrDms {

    private static final class Lazy {
        private static final DateTimeFormatter dmsDocDf = DateTimeFormatter.ofPattern("yyyyMMdd");
        private static final DateTimeFormatter dmsDocTf = DateTimeFormatter.ofPattern("HHmmss");
        private static final Pattern actDmsDocPattern = Pattern.compile("(\\w{8,16})-ACT-(\\d{8})-(\\d{8})\\.xml");
        private static final Pattern tcDmsDocPattern = Pattern.compile("(\\w{8,16})-TC-(\\d{8})-(\\d{6})\\.xml");
    }

    private final com.brinvex.util.dms.api.Dms dms;

    public IbkrDmsImpl(Dms dms) {
        this.dms = dms;
    }

    @Override
    public List<ActivityDocKey> getActivityDocKeys(String accountId, LocalDate fromDayIncl, LocalDate toDayIncl) {
        String directory = getDirectory(accountId);
        SequencedCollection<String> fileKeys = dms.getKeys(directory);
        List<ActivityDocKey> results = new ArrayList<>();
        Pattern p = Lazy.actDmsDocPattern;
        for (String fileKey : fileKeys) {
            Matcher m = p.matcher(fileKey);
            if (m.matches()) {
                String docAccountId = m.group(1);
                if (!docAccountId.equals(accountId)) {
                    throw new StorageException("Unexpected document '%s' in directory '%s'".formatted(fileKey, directory));
                }
                LocalDate docFromDayIncl = LocalDate.parse(m.group(2), Lazy.dmsDocDf);
                LocalDate docToDayIncl = LocalDate.parse(m.group(3), Lazy.dmsDocDf);
                boolean outside = (toDayIncl != null && docFromDayIncl.isAfter(toDayIncl)) || (fromDayIncl != null && docToDayIncl.isBefore(fromDayIncl));
                if (outside) {
                    continue;
                }
                results.add(new ActivityDocKey(docAccountId, docFromDayIncl, docToDayIncl));
            }
        }
        results.sort(naturalOrder());
        return results;
    }

    @Override
    public List<TradeConfirmDocKey> getTradeConfirmDocKeys(String accountId, LocalDate fromDayIncl, LocalDate toDayIncl) {
        String directory = getDirectory(accountId);
        SequencedCollection<String> fileKeys = dms.getKeys(directory);
        List<TradeConfirmDocKey> results = new ArrayList<>();
        Pattern p = Lazy.tcDmsDocPattern;
        for (String fileKey : fileKeys) {
            Matcher m = p.matcher(fileKey);
            if (m.matches()) {
                String docAccountId = m.group(1);
                if (!docAccountId.equals(accountId)) {
                    throw new StorageException("Unexpected document '%s' in directory '%s'".formatted(fileKey, directory));
                }
                LocalDate docDay = LocalDate.parse(m.group(2), Lazy.dmsDocDf);
                boolean outside = (toDayIncl != null && docDay.isAfter(toDayIncl)) || (fromDayIncl != null && docDay.isBefore(fromDayIncl));
                if (outside) {
                    continue;
                }
                LocalTime docTime = LocalTime.parse(m.group(3), Lazy.dmsDocTf);
                results.add(new TradeConfirmDocKey(docAccountId, docDay, docTime));
            }
        }
        results.sort(naturalOrder());
        return results;
    }

    @Override
    public TradeConfirmDocKey getUniqueTradeConfirmDocKey(String accountId, LocalDate day) {
        List<TradeConfirmDocKey> docKeys = getTradeConfirmDocKeys(accountId, day, day);
        int docKeysSize = docKeys.size();
        return switch (docKeysSize) {
            case 0 -> null;
            case 1 -> docKeys.getFirst();
            default -> throw new StorageException((
                    "Expecting one but found %s tcDocKeys matching " +
                    "accountId=%s, day=%s")
                    .formatted(docKeysSize, accountId, day));
        };
    }

    @Override
    public String getStatementContent(IbkrDocKey docKey) {
        String directory = getDirectory(docKey.accountId());
        String fileKey = constructFileKey(docKey);
        return dms.getTextContent(directory, fileKey);
    }

    @Override
    public boolean putStatementIfUseful(ActivityDocKey docKey, String content) {
        String accountId = docKey.accountId();
        String directory = getDirectory(accountId);

        List<ActivityDocKey> oldDocKeys = getActivityDocKeys(accountId, docKey.fromDayIncl(), docKey.toDayIncl());
        String newFileKey = constructFileKey(docKey);
        List<ActivityDocKey> oldAndNewDocKeys = new ArrayList<>(oldDocKeys);
        oldAndNewDocKeys.add(docKey);
        Set<ActivityDocKey> uselessKeys = detectUselessActDocs(oldAndNewDocKeys);
        boolean newSaved;
        if (!uselessKeys.remove(docKey)) {
            dms.add(directory, newFileKey, content);
            newSaved = true;
        } else {
            newSaved = false;
        }
        for (ActivityDocKey uselessKey : uselessKeys) {
            dms.delete(directory, constructFileKey(uselessKey));
        }
        return newSaved;
    }

    @Override
    public boolean putStatement(IbkrDocKey docKey, String content) {
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
                    Lazy.dmsDocDf.format(actDocKey.fromDayIncl()),
                    Lazy.dmsDocDf.format(actDocKey.toDayIncl())
            );
            case TradeConfirmDocKey tcDocKey -> "%s-TC-%s-%s.xml".formatted(
                    tcDocKey.accountId(),
                    Lazy.dmsDocDf.format(tcDocKey.day()),
                    Lazy.dmsDocTf.format(tcDocKey.whenGenerated())
            );
        };
    }

    private Set<ActivityDocKey> detectUselessActDocs(List<ActivityDocKey> docKeys) {
        int size = docKeys.size();
        if (size <= 1) {
            return emptySet();
        }
        docKeys = docKeys.stream().sorted(comparing(ActivityDocKey::fromDayIncl).thenComparing(ActivityDocKey::toDayIncl)).toList();
        Set<ActivityDocKey> uselessDocs = new HashSet<>();
        int prevUsefulIndex = 0;
        for (int i = 0; i < size; i++) {
            ActivityDocKey midKey = docKeys.get(i);
            boolean useful;
            if (i == 0) {
                ActivityDocKey nextKey = docKeys.get(i + 1);
                useful = midKey.fromDayIncl().isBefore(nextKey.fromDayIncl());
            } else if (i == size - 1) {
                ActivityDocKey prevKey = docKeys.get(prevUsefulIndex);
                useful = midKey.toDayIncl().isAfter(prevKey.toDayIncl());
            } else {
                ActivityDocKey prevKey = docKeys.get(prevUsefulIndex);
                ActivityDocKey nextKey = docKeys.get(i + 1);
                LocalDate prevToDayExcl = prevKey.toDayIncl().plusDays(1);
                boolean neighborsContinuous = !prevToDayExcl.isBefore(nextKey.fromDayIncl());
                if (neighborsContinuous) {
                    boolean inside = !midKey.fromDayIncl().isBefore(prevKey.fromDayIncl()) && !midKey.toDayIncl().isAfter(nextKey.toDayIncl());
                    useful = !inside;
                } else {
                    boolean insidePrev = !midKey.fromDayIncl().isBefore(prevKey.fromDayIncl()) && !midKey.toDayIncl().isAfter(prevKey.toDayIncl());
                    boolean insideNext = !midKey.fromDayIncl().isBefore(nextKey.fromDayIncl()) && !midKey.toDayIncl().isAfter(nextKey.toDayIncl());
                    useful = !insidePrev || !insideNext;
                }
            }
            if (!useful) {
                uselessDocs.add(midKey);
            } else {
                prevUsefulIndex = i;
            }
        }
        return uselessDocs;
    }

    private String getDirectory(String accountId) {
        return accountId;
    }

}
