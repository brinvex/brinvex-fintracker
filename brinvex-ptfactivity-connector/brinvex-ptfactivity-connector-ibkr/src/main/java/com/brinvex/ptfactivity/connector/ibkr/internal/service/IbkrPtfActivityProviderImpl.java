package com.brinvex.ptfactivity.connector.ibkr.internal.service;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.finance.types.vo.DateAmount;
import com.brinvex.ptfactivity.connector.ibkr.api.model.IbkrAccount;
import com.brinvex.ptfactivity.connector.ibkr.api.model.IbkrAccount.Credentials;
import com.brinvex.ptfactivity.connector.ibkr.api.model.IbkrDocKey.ActivityDocKey;
import com.brinvex.ptfactivity.connector.ibkr.api.model.IbkrDocKey.TradeConfirmDocKey;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.EquitySummary;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.FlexStatement.ActivityStatement;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.FlexStatement.TradeConfirmStatement;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrDms;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrFetcher;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrFinTransactionMapper;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrPtfActivityProvider;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrStatementMerger;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrStatementParser;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import com.brinvex.ptfactivity.core.api.domain.PtfActivity;
import com.brinvex.ptfactivity.core.api.domain.PtfActivityReq;
import com.brinvex.ptfactivity.core.api.exception.AssistanceRequiredException;
import com.brinvex.java.validation.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import static com.brinvex.java.collection.CollectionUtil.getFirstThrowIfMore;
import static com.brinvex.java.DateUtil.maxDate;
import static com.brinvex.java.DateUtil.minDate;
import static com.brinvex.java.NullUtil.nullSafe;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;

@SuppressWarnings("DuplicatedCode")
public class IbkrPtfActivityProviderImpl implements IbkrPtfActivityProvider {

    private final IbkrDms dms;

    private final IbkrStatementParser parser;

    private final IbkrFetcher fetcher;

    private final IbkrStatementMerger statementMerger;

    private final IbkrFinTransactionMapper finTransactionMapper;

    public IbkrPtfActivityProviderImpl(
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
    public boolean supports(PtfActivityReq ptfActivityReq) {
        return ptfActivityReq.providerName().equals("ibkr");
    }

    @Override
    public PtfActivity process(PtfActivityReq ptfActivityReq) {
        Assert.isTrue(supports(ptfActivityReq));
        IbkrAccount ibkrAccount = IbkrAccount.of(ptfActivityReq.account());
        LocalDate reqFromDateIncl = ptfActivityReq.fromDateIncl();
        LocalDate reqToDateIncl = ptfActivityReq.toDateIncl();
        Duration staleTolerance = ptfActivityReq.staleTolerance();
        return getPtfProgress(ibkrAccount, reqFromDateIncl, reqToDateIncl, staleTolerance);
    }

    @Override
    public PtfActivity getPtfProgressOffline(IbkrAccount ibkrAccount, LocalDate fromDateIncl, LocalDate toDateIncl) {
        return getPtfProgress(ibkrAccount, fromDateIncl, toDateIncl, null, false);
    }

    @Override
    public PtfActivity getPtfProgress(IbkrAccount account, LocalDate fromDateIncl, LocalDate toDateIncl, Duration staleTolerance) {
        return getPtfProgress(account, fromDateIncl, toDateIncl, staleTolerance, true);
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private PtfActivity getPtfProgress(IbkrAccount account, LocalDate fromDateIncl, LocalDate toDateIncl, Duration staleTolerance, boolean online) {
        LOG.debug("getPtfProgress({}, {}-{}, staleTolerance={}, online={})", account, fromDateIncl, toDateIncl, staleTolerance, online);

        List<FinTransaction> trans = new ArrayList<>();
        SequencedMap<LocalDate, DateAmount> navs = new LinkedHashMap<>();
        Currency pcy = account.ccy();

        for (IbkrAccount.MigratedAccount migratedAccount : account.migratedAccounts()) {
            LocalDate progressFromDateIncl = maxDate(fromDateIncl, migratedAccount.externalIdValidFromIncl());
            LocalDate progressToDateIncl = minDate(toDateIncl, migratedAccount.externalIdValidToIncl());
            if (!progressFromDateIncl.isAfter(progressToDateIncl)) {
                PtfActivity migrPtfActivity = getSinglePtfProgress(
                        migratedAccount.externalId(), pcy, migratedAccount.credentials(), progressFromDateIncl, progressToDateIncl, staleTolerance, online
                );
                if (migrPtfActivity != null) {
                    trans.addAll(migrPtfActivity.transactions());
                    for (DateAmount e : migrPtfActivity.netAssetValues()) {
                        navs.put(e.date(), e);
                    }
                }
            }
        }
        {
            LocalDate progressFromDateIncl = maxDate(fromDateIncl, account.externalIdValidFromIncl());
            LocalDate progressToDateIncl = toDateIncl;
            if (!progressFromDateIncl.isAfter(progressToDateIncl)) {
                PtfActivity mainPtfActivity = getSinglePtfProgress(
                        account.externalId(), pcy, account.credentials(), progressFromDateIncl, progressToDateIncl, staleTolerance, online
                );
                if (mainPtfActivity != null) {
                    trans.addAll(mainPtfActivity.transactions());
                    for (DateAmount e : mainPtfActivity.netAssetValues()) {
                        navs.put(e.date(), e);
                    }
                }
            }

        }
        if (navs.isEmpty()) {
            throw new IllegalStateException(
                    "Missing PtfProgress data: externalId=%s, fromDayIncl=%s, toDayIncl=%s, credentials=%s");
        }
        trans.sort(comparing(FinTransaction::date));

        return new PtfActivity(trans, new ArrayList<>(navs.values()));
    }

    private PtfActivity getSinglePtfProgress(String accountId, Currency pcy, Credentials credentials, LocalDate fromDateIncl, LocalDate toDateIncl, Duration staleTolerance, boolean online) {
        assert accountId != null;
        assert pcy != null;
        assert !fromDateIncl.isAfter(toDateIncl);

        LocalDateTime now = LocalDateTime.now();
        LocalDate ibkrToday = ZonedDateTime.now(IBKR_ZONE_ID).toLocalDate();
        {
            boolean actOnline = online && credentials != null && credentials.activityFlexQueryId() != null;
            if (actOnline) {
                Assert.notNull(staleTolerance, () -> "staleTolerance must not be null");
                LocalDate fetchFromDateIncl;
                List<ActivityDocKey> oldActDocKeys = dms.getActivityDocKeys(accountId, fromDateIncl, toDateIncl);
                ActivityDocKey newestOldKey;
                if (oldActDocKeys.isEmpty()) {
                    fetchFromDateIncl = fromDateIncl;
                    newestOldKey = null;
                } else {
                    LocalDate oldestOldDate = oldActDocKeys.getFirst().fromDateIncl();
                    newestOldKey = oldActDocKeys.getLast();
                    LocalDate newestOldDate = newestOldKey.toDateIncl();
                    if (fromDateIncl.isBefore(oldestOldDate)) {
                        fetchFromDateIncl = fromDateIncl;
                    } else if (toDateIncl.isAfter(newestOldDate)) {
                        fetchFromDateIncl = newestOldDate.plusDays(1);
                    } else {
                        fetchFromDateIncl = null;
                    }
                }
                if (fetchFromDateIncl != null && fetchFromDateIncl.isBefore(ibkrToday)) {
                    if (fetchFromDateIncl.isBefore(ibkrToday.minusDays(365))) {
                        throw new AssistanceRequiredException(
                                "IBKR does not allow automatic fetching of Flex Activity Statements for periods starting more than 365 days ago. " +
                                "Please manually retrieve the missed statements and upload them to DMS. " +
                                "externalId=%s, activityFlexQueryId=%s, fromDateIncl=%s, toDateIncl=%s, fetchFromDateIncl=%s, ibkrToday=%s"
                                        .formatted(accountId, credentials.activityFlexQueryId(), fromDateIncl, toDateIncl, fetchFromDateIncl, ibkrToday));
                    }


                    LocalDateTime newestOldCreatedOn = newestOldKey == null ? null :
                            parser.parseStatementCreatedOn(dms.getStatementContentLines(newestOldKey, 3));
                    if (newestOldCreatedOn == null || newestOldCreatedOn.plus(staleTolerance).isBefore(now)) {
                        String fetchedContent = fetcher.fetchFlexStatement(credentials.token(), credentials.activityFlexQueryId(), 4, ofSeconds(5));

                        ActivityStatement fetchedActStatement = parser.parseActivityStatement(fetchedContent);
                        ActivityDocKey fetchedDocKey = new ActivityDocKey(accountId, fetchedActStatement.fromDate(), fetchedActStatement.toDate());
                        boolean useful = dms.putActivityStatement(fetchedDocKey, fetchedContent);
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
                        LOG.debug("getSinglePtfProgress - skipping activity fetch - externalId={}, newestOldCreatedOn={}, staleTolerance={}, now={}",
                                accountId, newestOldCreatedOn, staleTolerance, now);
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
            throw new IllegalStateException("Given externalId=%s does not match foundAccountId=%s"
                    .formatted(accountId, mergedActStatement.accountId()));
        }
        if (mergedActStatement.fromDate().isAfter(fromDateIncl)) {
            throw new IllegalStateException("No Activity statement available for externalId=%s, fromDateIncl=%s"
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
                        TradeConfirmDocKey oldTcDocKey = getFirstThrowIfMore(dms.getTradeConfirmDocKeys(accountId, tcDate, tcDate));
                        if (oldTcDocKey != null) {
                            LocalDateTime oldCreatedOn = parser.parseStatementCreatedOn(dms.getStatementContentLines(oldTcDocKey, 3));
                            boolean oldIsStale = oldCreatedOn.plus(staleTolerance).isBefore(now);
                            if (oldIsStale) {
                                LOG.debug("getSinglePtfProgress - old is stale - oldTcDocKey={}, staleTolerance={}, now={}", oldTcDocKey, staleTolerance, now);
                            } else {
                                LOG.debug("getSinglePtfProgress - going to use old - oldTcDocKey={}, staleTolerance={}, now={}", oldTcDocKey, staleTolerance, now);
                                tcStatement = parser.parseTradeConfirmStatement(dms.getStatementContent(oldTcDocKey));
                            }
                        }
                        if (tcStatement == null) {
                            if (oldTcDocKey != null) {
                                dms.delete(oldTcDocKey);
                            }
                            String tcContent = fetcher.fetchFlexStatement(credentials.token(), credentials.tradeConfirmFlexQueryId(), 2, ofMillis(250));
                            tcStatement = parser.parseTradeConfirmStatement(tcContent);
                            dms.putTradeConfirmStatement(new TradeConfirmDocKey(accountId, tcStatement.fromDate()), tcContent);
                        }
                    }
                }
                if (tcStatement == null) {
                    tcStatement = nullSafe(
                            getFirstThrowIfMore(dms.getTradeConfirmDocKeys(accountId, tcDate, tcDate)),
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
        {
            for (EquitySummary equitySummary : mergedActStatement.equitySummaries()) {
                Assert.equal(pcy, equitySummary.currency());
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
        return new PtfActivity(newTrans, List.copyOf(navs.values()));
    }


    private static final Logger LOG = LoggerFactory.getLogger(IbkrPtfActivityProviderImpl.class);

    private static final ZoneId IBKR_ZONE_ID = ZoneId.of("America/New_York");
}
