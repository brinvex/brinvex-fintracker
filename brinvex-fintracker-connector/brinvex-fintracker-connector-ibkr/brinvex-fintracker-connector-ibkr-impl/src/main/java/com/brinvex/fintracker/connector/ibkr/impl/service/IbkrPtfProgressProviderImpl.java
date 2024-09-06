package com.brinvex.fintracker.connector.ibkr.impl.service;

import com.brinvex.fintracker.core.api.exception.AssistanceRequiredException;
import com.brinvex.fintracker.core.api.model.domain.FinTransaction;
import com.brinvex.fintracker.core.api.model.domain.PtfProgress;
import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.core.api.provider.PtfProgressProvider;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrAccount;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrAccount.Credentials;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrDocKey.ActivityDocKey;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrDocKey.TradeConfirmDocKey;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.EquitySummary;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.FlexStatement.ActivityStatement;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.FlexStatement.TradeConfirmStatement;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrDms;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrFetcher;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrFinTransactionMapper;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrPtfProgressProvider;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrStatementMerger;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrStatementParser;
import com.brinvex.util.java.validation.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import static com.brinvex.util.java.NullUtil.nullSafe;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNullElse;

@SuppressWarnings("DuplicatedCode")
public class IbkrPtfProgressProviderImpl implements IbkrPtfProgressProvider, PtfProgressProvider {

    private static final Logger LOG = LoggerFactory.getLogger(IbkrPtfProgressProviderImpl.class);

    private static final ZoneId IBKR_ZONE_ID = ZoneId.of("America/New_York");

    private static final String META_FILE_NAME = IbkrPtfProgressProviderImpl.class.getSimpleName() + ".properties";
    private static final String META_PROP_KEY_ACT_LAST_FETCHED = "activityLastFetched";

    private final IbkrDms dms;

    private final IbkrStatementParser parser;

    private final IbkrFetcher fetcher;

    private final IbkrStatementMerger statementMerger;

    private final IbkrFinTransactionMapper finTransactionMapper;

    public IbkrPtfProgressProviderImpl(
            IbkrDms dms,
            IbkrStatementParser parser,
            IbkrFetcher fetcher,
            IbkrStatementMerger statementMerger,
            IbkrFinTransactionMapper finTransactionMapper
    ) {
        this.dms = dms;
        this.parser = parser;
        this.fetcher = fetcher;
        this.statementMerger = statementMerger;
        this.finTransactionMapper = finTransactionMapper;
    }

    @Override
    public boolean supports(PtfProgressReq ptfProgressReq) {
        return ptfProgressReq.institution().equals("IBKR");
    }

    @Override
    public PtfProgress process(PtfProgressReq ptfProgressReq) {
        Assert.equal(ptfProgressReq.institution(), "IBKR");
        Map<String, String> ptfProps = ptfProgressReq.ptfProps();

        IbkrAccount ibkrAccount = IbkrAccount.of(ptfProps);
        LocalDate reqFromDateIncl = ptfProgressReq.fromDateIncl();
        LocalDate reqToDateIncl = ptfProgressReq.toDateIncl();
        Duration staleTolerance = ptfProgressReq.staleTolerance();

        return getPortfolioProgress(ibkrAccount, reqFromDateIncl, reqToDateIncl, staleTolerance);
    }

    @Override
    public PtfProgress getPortfolioProgressOffline(IbkrAccount ibkrAccount, LocalDate fromDateIncl, LocalDate toDateIncl) {
        return getPtfProgress(ibkrAccount, fromDateIncl, toDateIncl, null, false, 0);
    }

    @Override
    public PtfProgress getPortfolioProgress(IbkrAccount account, LocalDate fromDateIncl, LocalDate toDateIncl, Duration staleTolerance) {
        return getPtfProgress(account, fromDateIncl, toDateIncl, staleTolerance, true, 0);
    }

    private PtfProgress getPtfProgress(IbkrAccount account, LocalDate fromDateIncl, LocalDate toDateIncl, Duration staleTolerance, boolean online, int recursionDepth) {
        LOG.debug("getPtfProgress({}, {}-{}, staleTolerance={}, online={}, recursionDepth={})", account, fromDateIncl, toDateIncl, staleTolerance, online, recursionDepth);
        Assert.isTrue(recursionDepth < 10, () -> "getPtfProgress - recursionDepth must me less then 10, %s, %s-%s, staleTolerance=%s, online=%s, recursionDepth=%s)"
                .formatted(account, fromDateIncl, toDateIncl, staleTolerance, online, recursionDepth));
        IbkrAccount.MigratedAccount migratedAccount = account.migratedAccount();

        PtfProgress resultProgress;
        if (migratedAccount == null) {
            resultProgress = getSinglePtfProgress(account.accountId(), account.credentials(), fromDateIncl, toDateIncl, staleTolerance, online);
        } else {
            PtfProgress migratedProgress;
            PtfProgress progress;

            LocalDate migrationFromIncl = migratedAccount.migrationFromIncl();
            LocalDate migrationToIncl = migratedAccount.migrationToIncl();
            if (fromDateIncl.isAfter(migrationToIncl)) {
                migratedProgress = null;
                progress = getSinglePtfProgress(account.accountId(), account.credentials(), fromDateIncl, toDateIncl, staleTolerance, online);
            } else if (toDateIncl.isBefore(migrationFromIncl)) {
                migratedProgress = getPtfProgress(migratedAccount.oldAccount(), fromDateIncl, toDateIncl, staleTolerance, online, recursionDepth + 1);
                progress = null;
            } else {
                migratedProgress = getPtfProgress(migratedAccount.oldAccount(), fromDateIncl, migrationToIncl, staleTolerance, online, recursionDepth + 1);
                progress = getSinglePtfProgress(account.accountId(), account.credentials(), migrationFromIncl, toDateIncl, staleTolerance, online);
            }
            if (progress != null) {
                if (migratedProgress != null) {
                    String resultCcy = progress.ccy();
                    Assert.equal(resultCcy, migratedProgress.ccy());
                    List<FinTransaction> resultTrans = Stream
                            .concat(
                                    migratedProgress.transactions().stream(),
                                    progress.transactions().stream())
                            .sorted(comparing(FinTransaction::date))
                            .toList();
                    List<DateAmount> resultNavs = Stream
                            .concat(
                                    migratedProgress.netAssetValues().stream().filter(e -> !e.date().isAfter(migrationFromIncl)),
                                    progress.netAssetValues().stream().filter(e -> e.date().isAfter(migrationToIncl)))
                            .sorted(comparing(DateAmount::date))
                            .toList();
                    resultProgress = new PtfProgress(resultTrans, resultNavs, resultCcy);
                } else {
                    resultProgress = progress;
                }
            } else {
                resultProgress = migratedProgress;
            }
        }

        if (resultProgress == null) {
            throw new IllegalStateException(
                    "Missing PtfProgress data: accountId=%s, fromDayIncl=%s, toDayIncl=%s, migratedAccount=%s, credentials=%s"
                            .formatted(account.accountId(), fromDateIncl, toDateIncl, account.migratedAccount(), account.credentials() == null));
        }
        return resultProgress;
    }

    private PtfProgress getSinglePtfProgress(String accountId, Credentials credentials, LocalDate fromDateIncl, LocalDate toDateIncl, Duration staleTolerance, boolean online) {
        Assert.notNull(accountId, () -> "accountId must not be null");
        Assert.isTrue(!fromDateIncl.isAfter(toDateIncl));

        LocalDate ibkrToday = ZonedDateTime.now(IBKR_ZONE_ID).toLocalDate();
        {
            boolean actOnline = online && credentials != null && credentials.activityFlexQueryId() != null;
            if (actOnline) {
                Assert.notNull(staleTolerance, () -> "staleTolerance must not be null");
                LocalDate fetchFromDateIncl;
                List<ActivityDocKey> oldActDocKeys = dms.getActivityDocKeys(accountId, fromDateIncl, toDateIncl);
                if (oldActDocKeys.isEmpty()) {
                    fetchFromDateIncl = fromDateIncl;
                } else {
                    LocalDate oldOldestDate = oldActDocKeys.getFirst().fromDateIncl();
                    LocalDate oldNewestDate = oldActDocKeys.getLast().toDateIncl();
                    if (fromDateIncl.isBefore(oldOldestDate)) {
                        fetchFromDateIncl = fromDateIncl;
                    } else if (toDateIncl.isAfter(oldNewestDate)) {
                        fetchFromDateIncl = oldNewestDate.plusDays(1);
                    } else {
                        fetchFromDateIncl = null;
                    }
                }
                if (fetchFromDateIncl != null && fetchFromDateIncl.isBefore(ibkrToday)) {
                    if (fetchFromDateIncl.isBefore(ibkrToday.minusDays(365))) {
                        throw new AssistanceRequiredException(
                                "IBKR does not allow automatic fetching of Flex Activity Statements for periods starting more than 365 days ago. " +
                                "Please manually retrieve the missed statements and upload them to DMS. " +
                                "accountId=%s, activityFlexQueryId=%s, fromDateIncl=%s, toDateIncl=%s, fetchFromDateIncl=%s, ibkrToday=%s"
                                        .formatted(accountId, credentials.activityFlexQueryId(), fromDateIncl, toDateIncl, fetchFromDateIncl, ibkrToday));
                    }

                    LocalDateTime now = LocalDateTime.now();
                    Map<String, String> metaProperties = dms.getMetaProperties(accountId, META_FILE_NAME);
                    LocalDateTime actLastFetched = requireNonNullElse(
                            nullSafe(metaProperties.get(META_PROP_KEY_ACT_LAST_FETCHED), LocalDateTime::parse),
                            LocalDateTime.MIN);
                    boolean actStale = actLastFetched.plus(staleTolerance).isBefore(now);
                    if (actStale) {
                        String fetchedContent = fetcher.fetchFlexStatement(credentials.token(), credentials.activityFlexQueryId(), 4, ofSeconds(5));
                        metaProperties = new LinkedHashMap<>(metaProperties);
                        metaProperties.put(META_PROP_KEY_ACT_LAST_FETCHED, now.toString());
                        dms.putMetaProperties(accountId, META_FILE_NAME, metaProperties);

                        ActivityStatement fetchedActStatement = parser.parseActivityStatement(fetchedContent);
                        ActivityDocKey fetchedDocKey = new ActivityDocKey(accountId, fetchedActStatement.fromDate(), fetchedActStatement.toDate());
                        boolean useful = dms.putStatementIfUseful(fetchedDocKey, fetchedContent);
                        if (useful) {
                            LOG.debug("getSinglePtfProgress - saved fetched statement - {}", fetchedDocKey);
                        } else {
                            LOG.debug("getSinglePtfProgress - fetched statement is useless - {}", fetchedDocKey);
                        }
                        {
                            List<TradeConfirmDocKey> unnecessaryTcDocKeys = dms.getTradeConfirmDocKeys(accountId, fetchedActStatement.fromDate(), fetchedActStatement.toDate());
                            for (TradeConfirmDocKey unnecessaryTcDocKey : unnecessaryTcDocKeys) {
                                LOG.debug("getPortfolioProgress - deleting unnecessary statement - {}", unnecessaryTcDocKey);
                                dms.delete(unnecessaryTcDocKey);
                            }
                        }
                    } else {
                        LOG.debug("getSinglePtfProgress - skipping activity fetch - accountId={}, actLastFetched={}, staleTolerance={}, now={}",
                                accountId, actLastFetched, staleTolerance, now);
                    }
                }
            }
        }
        List<ActivityStatement> actStatements = dms.getActivityDocKeys(accountId, fromDateIncl, toDateIncl)
                .stream()
                .map(dms::getStatementContent)
                .map(parser::parseActivityStatement)
                .toList();

        ActivityStatement mergedActStatement = statementMerger.mergeActivityStatements(actStatements).orElse(null);
        if (mergedActStatement == null) {
            return null;
        }

        if (!accountId.equals(mergedActStatement.accountId())) {
            throw new IllegalStateException("Given accountId=%s does not match foundAccountId=%s"
                    .formatted(accountId, mergedActStatement.accountId()));
        }
        if (mergedActStatement.fromDate().isAfter(fromDateIncl)) {
            throw new IllegalStateException("No Activity statement available for accountId=%s, fromDateIncl=%s"
                    .formatted(accountId, fromDateIncl));
        }

        List<FinTransaction> cashTrans = finTransactionMapper.mapCashTransactions(mergedActStatement.cashTransactions());

        List<FinTransaction> trades = finTransactionMapper.mapTrades(mergedActStatement.trades());

        List<FinTransaction> corpActions = finTransactionMapper.mapCorporateAction(mergedActStatement.corporateActions());

        List<FinTransaction> tcTrades;
        {
            LocalDate tcDate = mergedActStatement.toDate().plusDays(1);
            TradeConfirmStatement tcStatement = null;
            if (!tcDate.isAfter(toDateIncl)) {
                boolean tcOnline = online && credentials != null && credentials.tradeConfirmFlexQueryId() != null;
                if (tcOnline) {
                    if (tcDate.isEqual(ibkrToday)) {
                        TradeConfirmDocKey oldTcDocKey = dms.getUniqueTradeConfirmDocKey(accountId, tcDate);
                        if (oldTcDocKey != null) {
                            ZonedDateTime ibkrNow = ZonedDateTime.now(IBKR_ZONE_ID);
                            ZonedDateTime oldWhenGenerated = ZonedDateTime.of(oldTcDocKey.date(), oldTcDocKey.whenGenerated(), IBKR_ZONE_ID);
                            boolean oldIsStale = oldWhenGenerated.plus(staleTolerance).isBefore(ibkrNow);
                            if (oldIsStale) {
                                LOG.debug("getSinglePtfProgress - old is stale - oldTcDocKey={}, staleTolerance={}, ibkrNow={}", oldTcDocKey, staleTolerance, ibkrNow);
                            } else {
                                LOG.debug("getSinglePtfProgress - going to use old - oldTcDocKey={}, staleTolerance={}, ibkrNow={}", oldTcDocKey, staleTolerance, ibkrNow);
                                tcStatement = parser.parseTradeConfirmStatement(dms.getStatementContent(oldTcDocKey));
                            }
                        }
                        if (tcStatement == null) {
                            if (oldTcDocKey != null) {
                                dms.delete(oldTcDocKey);
                            }
                            String tcContent = fetcher.fetchFlexStatement(credentials.token(), credentials.tradeConfirmFlexQueryId(), 2, ofMillis(250));
                            tcStatement = parser.parseTradeConfirmStatement(tcContent);
                            dms.putStatement(new TradeConfirmDocKey(accountId, tcStatement.fromDate(), tcStatement.whenGenerated().toLocalTime()), tcContent);
                        }
                    }
                }
                if (tcStatement == null) {
                    tcStatement = nullSafe(
                            dms.getUniqueTradeConfirmDocKey(accountId, tcDate),
                            dms::getStatementContent,
                            parser::parseTradeConfirmStatement
                    );
                }
            }
            if (tcStatement != null) {
                tcTrades = finTransactionMapper.mapTradeConfirms(tcStatement.tradeConfirmations());
            } else {
                tcTrades = emptyList();
            }
        }

        List<FinTransaction> newTrans = Stream.of(corpActions, cashTrans, trades, tcTrades)
                .flatMap(Collection::stream)
                .filter(t -> {
                    LocalDate date = t.date();
                    return !date.isBefore(fromDateIncl) && !date.isAfter(toDateIncl);
                })
                .sorted(comparing(FinTransaction::date))
                .toList();

        SortedMap<LocalDate, DateAmount> navs = new TreeMap<>();
        String navsCcy;
        {
            List<EquitySummary> eqSummaries = mergedActStatement.equitySummaries();
            navsCcy = eqSummaries.isEmpty() ? null : eqSummaries.getFirst().currency();
            for (EquitySummary equitySummary : eqSummaries) {
                Assert.equal(equitySummary.currency(), navsCcy, () ->
                        "Found multiple currencies while processing EquitySummary records: %s != %s, %s, accountId=%s, %s-%s"
                                .formatted(equitySummary.currency(), navsCcy, equitySummary, accountId, fromDateIncl, toDateIncl));

                LocalDate date = equitySummary.reportDate();
                if (date.isBefore(fromDateIncl) || date.isAfter(toDateIncl)) {
                    continue;
                }
                DateAmount old = navs.put(date, new DateAmount(date, equitySummary.total()));
                if (old != null) {
                    throw new IllegalStateException("Duplicate EquitySummary date: %s, %s".formatted(old, equitySummary));
                }
            }
        }
        return new PtfProgress(newTrans, List.copyOf(navs.values()), navsCcy);
    }

}
