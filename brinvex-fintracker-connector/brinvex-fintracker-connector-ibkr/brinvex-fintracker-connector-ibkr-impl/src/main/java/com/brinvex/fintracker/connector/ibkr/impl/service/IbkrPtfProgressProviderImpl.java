package com.brinvex.fintracker.connector.ibkr.impl.service;

import com.brinvex.fintracker.api.exception.AssistanceRequiredException;
import com.brinvex.fintracker.api.model.domain.FinTransaction;
import com.brinvex.fintracker.api.model.domain.PtfProgress;
import com.brinvex.fintracker.api.model.general.DateAmount;
import com.brinvex.fintracker.api.provider.PtfProgressProvider;
import com.brinvex.fintracker.connector.ibkr.api.model.IbkrCredentials;
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
import com.brinvex.util.java.validation.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import static com.brinvex.util.java.NullUtil.nullSafe;
import static com.brinvex.util.java.StringUtil.stripToNull;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNullElse;

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

        String accountId = ptfProps.get(IbkrPtfPropDef.ACCOUNT_ID);
        String token = stripToNull(ptfProps.get(IbkrPtfPropDef.TOKEN));
        String actFlexQueryId = stripToNull(ptfProps.get(IbkrPtfPropDef.ACTIVITY_FLEX_QUERY_ID));
        String tcFlexQueryId = stripToNull(ptfProps.get(IbkrPtfPropDef.TRADE_CONFIRM_FLEX_QUERY_ID));
        PtfProgress ptfProgress;
        if (actFlexQueryId != null || tcFlexQueryId != null) {
            IbkrCredentials credentials = new IbkrCredentials(token, actFlexQueryId, tcFlexQueryId);
            ptfProgress = getPortfolioProgressOnline(
                    accountId,
                    credentials,
                    ptfProgressReq.fromDateIncl(),
                    ptfProgressReq.toDateIncl(),
                    ptfProgressReq.staleTolerance());
        } else {
            ptfProgress = getPortfolioProgressOffline(
                    accountId,
                    ptfProgressReq.fromDateIncl(),
                    ptfProgressReq.toDateIncl());
        }
        return ptfProgress;
    }

    @Override
    public PtfProgress getPortfolioProgressOnline(
            String accountId,
            IbkrCredentials credentials,
            LocalDate fromDateIncl,
            LocalDate toDateIncl,
            Duration staleTolerance
    ) {
        Validate.notNullNotBlank(accountId, () -> "accountId cannot be null or empty");
        Validate.notNull(credentials, () -> "credentials cannot be null");
        Validate.notNull(fromDateIncl, () -> "fromDateIncl cannot be null");
        Validate.notNull(toDateIncl, () -> "toDateIncl cannot be null");
        Validate.notNull(staleTolerance, () -> "staleTolerance cannot be null");

        return getPortfolioProgress(accountId, credentials, fromDateIncl, toDateIncl, staleTolerance);
    }

    @Override
    public PtfProgress getPortfolioProgressOffline(
            String accountId,
            LocalDate fromDateIncl,
            LocalDate toDateIncl
    ) {
        Validate.notNullNotBlank(accountId, () -> "accountId cannot be null or empty");
        Validate.notNull(fromDateIncl, () -> "fromDateIncl cannot be null");
        Validate.notNull(toDateIncl, () -> "toDateIncl cannot be null");

        return getPortfolioProgress(accountId, null, fromDateIncl, toDateIncl, null);
    }

    private PtfProgress getPortfolioProgress(String accountId, IbkrCredentials credentials, LocalDate fromDateIncl, LocalDate toDateIncl, Duration staleTolerance) {

        LocalDate ibkrToday = ZonedDateTime.now(IBKR_ZONE_ID).toLocalDate();
        {
            boolean actOnline = credentials != null && credentials.activityFlexQueryId() != null;
            if (actOnline) {
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
                            LOG.debug("getPortfolioProgress - saved fetched statement - {}", fetchedDocKey);
                        } else {
                            LOG.debug("getPortfolioProgress - fetched statement is useless - {}", fetchedDocKey);
                        }
                        {
                            List<TradeConfirmDocKey> unnecessaryTcDocKeys = dms.getTradeConfirmDocKeys(accountId, fetchedActStatement.fromDate(), fetchedActStatement.toDate());
                            for (TradeConfirmDocKey unnecessaryTcDocKey : unnecessaryTcDocKeys) {
                                LOG.debug("getPortfolioProgress - deleting unnecessary statement - {}", unnecessaryTcDocKey);
                                dms.delete(unnecessaryTcDocKey);
                            }
                        }
                    } else {
                        LOG.debug("getPortfolioProgress - skipping activity fetch - accountId={}, actLastFetched={}, staleTolerance={}, now={}",
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

        List<FinTransaction> cashTrans = finTransactionMapper.mapCashTransactions(mergedActStatement.cashTransactions());

        List<FinTransaction> trades = finTransactionMapper.mapTrades(mergedActStatement.trades());

        List<FinTransaction> corpActions = finTransactionMapper.mapCorporateAction(mergedActStatement.corporateActions());

        List<FinTransaction> tcTrades;
        {
            LocalDate tcDate = mergedActStatement.toDate().plusDays(1);
            TradeConfirmStatement tcStatement = null;
            if (!tcDate.isAfter(toDateIncl)) {
                boolean tcOnline = credentials != null && credentials.tradeConfirmFlexQueryId() != null;
                if (tcOnline) {
                    if (tcDate.isEqual(ibkrToday)) {
                        TradeConfirmDocKey oldTcDocKey = dms.getUniqueTradeConfirmDocKey(accountId, tcDate);
                        if (oldTcDocKey != null) {
                            ZonedDateTime ibkrNow = ZonedDateTime.now(IBKR_ZONE_ID);
                            ZonedDateTime oldWhenGenerated = ZonedDateTime.of(oldTcDocKey.date(), oldTcDocKey.whenGenerated(), IBKR_ZONE_ID);
                            boolean oldIsStale = oldWhenGenerated.plus(staleTolerance).isBefore(ibkrNow);
                            if (oldIsStale) {
                                LOG.debug("getPortfolioProgress - old is stale - oldTcDocKey={}, staleTolerance={}, ibkrNow={}", oldTcDocKey, staleTolerance, ibkrNow);
                            } else {
                                LOG.debug("getPortfolioProgress - going to use old - oldTcDocKey={}, staleTolerance={}, ibkrNow={}", oldTcDocKey, staleTolerance, ibkrNow);
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
