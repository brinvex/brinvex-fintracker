package com.brinvex.fintracker.connector.ibkr.impl.service;

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
import com.brinvex.fintracker.core.api.exception.AssistanceRequiredException;
import com.brinvex.fintracker.core.api.facade.ValidatorFacade;
import com.brinvex.fintracker.core.api.model.domain.FinTransaction;
import com.brinvex.fintracker.core.api.model.domain.PtfProgress;
import com.brinvex.fintracker.core.api.model.domain.constraints.fintransaction.FinTransactionConstraints;
import com.brinvex.fintracker.core.api.model.general.DateAmount;
import com.brinvex.fintracker.core.api.provider.PtfProgressProvider;
import com.brinvex.util.java.validation.Assert;
import jakarta.validation.ConstraintViolation;
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
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import static com.brinvex.util.java.DateUtil.maxDate;
import static com.brinvex.util.java.DateUtil.minDate;
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

    private final ValidatorFacade validatorFacade;

    public IbkrPtfProgressProviderImpl(
            IbkrDms dms,
            IbkrStatementParser parser,
            IbkrFetcher fetcher,
            IbkrStatementMerger statementMerger,
            IbkrFinTransactionMapper finTransactionMapper,
            ValidatorFacade validatorFacade
    ) {
        this.dms = dms;
        this.parser = parser;
        this.fetcher = fetcher;
        this.statementMerger = statementMerger;
        this.finTransactionMapper = finTransactionMapper;
        this.validatorFacade = validatorFacade;
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
        return getPtfProgress(ibkrAccount, fromDateIncl, toDateIncl, null, false);
    }

    @Override
    public PtfProgress getPortfolioProgress(IbkrAccount account, LocalDate fromDateIncl, LocalDate toDateIncl, Duration staleTolerance) {
        return getPtfProgress(account, fromDateIncl, toDateIncl, staleTolerance, true);
    }

    private PtfProgress getPtfProgress(IbkrAccount account, LocalDate fromDateIncl, LocalDate toDateIncl, Duration staleTolerance, boolean online) {
        LOG.debug("getPtfProgress({}, {}-{}, staleTolerance={}, online={})", account, fromDateIncl, toDateIncl, staleTolerance, online);

        List<FinTransaction> trans = new ArrayList<>();
        SequencedMap<LocalDate, DateAmount> navs = new LinkedHashMap<>();
        String ccy = null;
        for (IbkrAccount.IdAccount idAccount : account.allIdAccountsAsc()) {
            LocalDate idValidFrom = idAccount.idValidFromIncl();
            LocalDate idValidToIncl = idAccount.idValidToIncl();
            LocalDate progressFromDateIncl = maxDate(fromDateIncl, idValidFrom);
            LocalDate progressToDateIncl = idValidToIncl == null ? toDateIncl : minDate(toDateIncl, idValidToIncl);

            if (!progressFromDateIncl.isAfter(progressToDateIncl)) {
                PtfProgress singlePtfProgress = getSinglePtfProgress(
                        idAccount.accountId(), idAccount.credentials(), progressFromDateIncl, progressToDateIncl, staleTolerance, online
                );
                if (singlePtfProgress != null) {
                    if (ccy == null) {
                        ccy = singlePtfProgress.ccy();
                    } else {
                        Assert.isTrue(ccy.equals(singlePtfProgress.ccy()));
                    }
                    trans.addAll(singlePtfProgress.transactions());
                    for (DateAmount e : singlePtfProgress.netAssetValues()) {
                        navs.put(e.date(), e);
                    }
                }
            }
        }
        if (navs.isEmpty()) {
            throw new IllegalStateException(
                    "Missing PtfProgress data: accountId=%s, fromDayIncl=%s, toDayIncl=%s, credentials=%s");
        }
        trans.sort(comparing(FinTransaction::date));
        PtfProgress resultProgress = new PtfProgress(trans, new ArrayList<>(navs.values()), ccy);

        for (FinTransaction finTransaction : resultProgress.transactions()) {
            Set<ConstraintViolation<FinTransactionConstraints>> violations = validatorFacade.validate(FinTransactionConstraints.of(finTransaction));
            Assert.isTrue(violations.isEmpty(), () -> "FinTransactionConstraints violations - accountId=%s, finTransaction=%s, violations=%s"
                    .formatted(account.accountId(), finTransaction, violations));
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
