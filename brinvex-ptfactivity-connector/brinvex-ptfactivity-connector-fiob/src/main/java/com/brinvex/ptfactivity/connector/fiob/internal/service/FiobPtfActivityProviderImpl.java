package com.brinvex.ptfactivity.connector.fiob.internal.service;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.finance.types.vo.DateAmount;
import com.brinvex.ptfactivity.connector.fiob.api.model.FiobAccountType;
import com.brinvex.ptfactivity.connector.fiob.api.model.FiobDocKey;
import com.brinvex.ptfactivity.connector.fiob.api.model.FiobDocKey.SavingTransDocKey;
import com.brinvex.ptfactivity.connector.fiob.api.model.FiobDocKey.SnapshotDocKey;
import com.brinvex.ptfactivity.connector.fiob.api.model.FiobDocKey.TradingSnapshotDocKey;
import com.brinvex.ptfactivity.connector.fiob.api.model.FiobDocKey.TradingTransDocKey;
import com.brinvex.ptfactivity.connector.fiob.api.model.FiobDocKey.TransDocKey;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.Statement.SavingTransStatement;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.Statement.TradingTransStatement;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.Statement.TransStatement;
import com.brinvex.ptfactivity.connector.fiob.api.service.FiobDms;
import com.brinvex.ptfactivity.connector.fiob.api.service.FiobFetcher;
import com.brinvex.ptfactivity.connector.fiob.api.service.FiobFinTransactionMapper;
import com.brinvex.ptfactivity.connector.fiob.api.service.FiobPtfActivityProvider;
import com.brinvex.ptfactivity.connector.fiob.api.service.FiobStatementMerger;
import com.brinvex.ptfactivity.connector.fiob.api.service.FiobStatementParser;
import com.brinvex.ptfactivity.core.api.domain.Account;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import com.brinvex.java.ThreadUtil;
import com.brinvex.java.validation.Assert;
import com.brinvex.ptfactivity.core.api.domain.PtfActivity;
import com.brinvex.ptfactivity.core.api.domain.PtfActivityReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static com.brinvex.ptfactivity.connector.fiob.api.model.FiobAccountType.SAVING;
import static com.brinvex.ptfactivity.connector.fiob.api.model.FiobAccountType.TRADING;
import static com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType.DEPOSIT;
import static com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType.WITHDRAWAL;
import static com.brinvex.java.collection.CollectionUtil.getFirstThrowIfMore;
import static com.brinvex.java.DateUtil.isLastDayOfMonth;
import static com.brinvex.java.DateUtil.minDate;
import static java.time.Duration.ofSeconds;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toSet;

@SuppressWarnings("DuplicatedCode")
public class FiobPtfActivityProviderImpl implements FiobPtfActivityProvider {

    private final FiobDms dms;

    private final FiobStatementParser parser;

    private final FiobFetcher fetcher;

    private final FiobStatementMerger statementMerger;

    private final FiobFinTransactionMapper finTransactionMapper;

    public FiobPtfActivityProviderImpl(
            FiobDms dms,
            FiobStatementParser parser,
            FiobFetcher fetcher,
            FiobStatementMerger statementMerger,
            FiobFinTransactionMapper finTransactionMapper
    ) {
        this.dms = dms;
        this.parser = parser;
        this.fetcher = fetcher;
        this.statementMerger = statementMerger;
        this.finTransactionMapper = finTransactionMapper;
    }

    @Override
    public boolean supports(PtfActivityReq ptfActivityReq) {
        return ptfActivityReq.providerName().equals("fiob");
    }

    @Override
    public PtfActivity process(PtfActivityReq ptfActivityReq) {
        Assert.isTrue(supports(ptfActivityReq));
        return getPtfProgress(ptfActivityReq.account(), ptfActivityReq.fromDateIncl(), ptfActivityReq.toDateIncl(), ptfActivityReq.staleTolerance());
    }

    @Override
    public PtfActivity getPtfProgressOffline(Account fiobAccount, LocalDate fromDateIncl, LocalDate toDateIncl) {
        return getPtfProgress(fiobAccount, fromDateIncl, toDateIncl, null, false);
    }

    @Override
    public PtfActivity getPtfProgress(
            Account account,
            LocalDate fromDateIncl,
            LocalDate toDateIncl,
            Duration staleTolerance
    ) {
        return getPtfProgress(account, fromDateIncl, toDateIncl, staleTolerance, true);
    }

    private PtfActivity getPtfProgress(
            Account account,
            LocalDate fromDateIncl,
            LocalDate toDateIncl,
            Duration staleTolerance,
            boolean online
    ) {
        return switch (FiobAccountType.valueOf(account.type())) {
            case SAVING -> getSavingPtfProgress(account, fromDateIncl, toDateIncl, staleTolerance, online);
            case TRADING -> getTradingPtfProgress(account, fromDateIncl, toDateIncl, staleTolerance, online);
        };
    }

    private PtfActivity getTradingPtfProgress(
            Account account,
            LocalDate fromDateIncl,
            LocalDate toDateIncl,
            Duration staleTolerance,
            boolean online
    ) {
        LOG.debug("getTradingPtfProgress({}, {}-{}, staleTolerance={}, online={})", account, fromDateIncl, toDateIncl, staleTolerance, online);
        assert TRADING.name().equals(account.type());
        String accountId = account.externalId();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        Assert.isTrue(!fromDateIncl.isAfter(toDateIncl));

        online = online && account.credentials() != null;

        FiobFetcher fetcher = online ? this.fetcher.newSessionReusingFetcher() : null;

        if (online) {
            Assert.notNull(staleTolerance, () -> "staleTolerance must not be null");
            LocalDate fetchFromDateIncl = fromDateIncl.getDayOfYear() < 10 ? fromDateIncl.withDayOfYear(1).minusYears(1) : fromDateIncl.withDayOfYear(1);
            LocalDate fetchToDateIncl = minDate(today, toDateIncl);
            for (LocalDate iterFromDateIncl = fetchFromDateIncl;
                 !iterFromDateIncl.isAfter(fetchToDateIncl);
                 iterFromDateIncl = iterFromDateIncl.plusYears(1)
            ) {
                LocalDate iterToDateIncl = minDate(fetchToDateIncl, iterFromDateIncl.plusYears(1).minusDays(1));
                assert !iterFromDateIncl.isAfter(iterToDateIncl);

                TransDocKey docKey = new TradingTransDocKey(accountId, iterFromDateIncl, iterToDateIncl);
                {
                    List<String> oldStatementHeaderLines = dms.getStatementContentLinesIfExists(docKey, 2);
                    if (oldStatementHeaderLines != null) {
                        LocalDateTime oldStatementCreatedOn = parser.parseTradingStatementCreatedOn(oldStatementHeaderLines);
                        if (iterToDateIncl.plusDays(10).isBefore(oldStatementCreatedOn.toLocalDate())) {
                            continue;
                        }
                        boolean oldIsStale = oldStatementCreatedOn.plus(staleTolerance).isBefore(now);
                        if (!oldIsStale) {
                            continue;
                        }
                    }
                }
                {
                    TradingTransDocKey overlappingDocKey = getFirstThrowIfMore(dms.getTradingTransDocKeys(accountId, iterFromDateIncl, iterFromDateIncl));
                    if (overlappingDocKey != null) {
                        if (!overlappingDocKey.toDateIncl().isBefore(iterToDateIncl)) {
                            continue;
                        }
                    }
                }

                if (iterFromDateIncl != fetchFromDateIncl) {
                    ThreadUtil.sleep(ofSeconds(1));
                }

                String fetchedContent = fetcher.fetchTransStatement(account, iterFromDateIncl, iterToDateIncl);

                boolean useful = dms.putStatement(docKey, fetchedContent);
                if (useful) {
                    LOG.debug("getTradingPtfProgress - saved fetched trans statement - {}", docKey);
                } else {
                    LOG.warn("getTradingPtfProgress - fetched trans statement is useless - {}", docKey);
                }
            }
        }

        List<FinTransaction> newFinTrans;
        Set<LocalDate> newDepositWithdrawalDates;
        {
            List<? extends FiobDocKey> transDocKeys = dms.getTransDocKeys(accountId, TRADING, null, toDateIncl);
            if (transDocKeys.isEmpty()) {
                throw new IllegalStateException("No TransStatement available for externalId=%s, toDateIncl=%s"
                        .formatted(accountId, toDateIncl));
            }
            List<TradingTransStatement> transStatements = transDocKeys
                    .stream()
                    .map(dms::getStatementContent)
                    .map(parser::parseTradingTransStatement)
                    .toList();

            TransStatement mergedTransStatement = statementMerger.mergeTradingTransStatements(transStatements).orElseThrow();

            if (!accountId.equals(mergedTransStatement.accountId())) {
                throw new IllegalStateException("Given externalId=%s does not match foundAccountId=%s"
                        .formatted(accountId, mergedTransStatement.accountId()));
            }
            if (mergedTransStatement.periodFrom().isAfter(fromDateIncl)) {
                throw new IllegalStateException("No TransStatement available for externalId=%s, fromDateIncl=%s"
                        .formatted(accountId, fromDateIncl));
            }

            List<FinTransaction> finTrans = finTransactionMapper.mapTransactions(mergedTransStatement);
            newFinTrans = finTrans
                    .stream()
                    .filter(t -> !t.date().isBefore(fromDateIncl) && !t.date().isAfter(toDateIncl))
                    .sorted(comparing(FinTransaction::date))
                    .toList();

            newDepositWithdrawalDates = newFinTrans
                    .stream()
                    .filter(ft -> ft.type().equals(DEPOSIT) || ft.type().equals(WITHDRAWAL))
                    .map(FinTransaction::date)
                    .collect(toSet());
        }

        if (online) {
            List<LocalDate> snapshotDates = fromDateIncl
                    .datesUntil(toDateIncl.plusDays(1))
                    .filter(d -> d.isEqual(toDateIncl)
                                 || newDepositWithdrawalDates.contains(d)
                                 || newDepositWithdrawalDates.contains(d.minusDays(1))
                                 || isLastDayOfMonth(d)
                    )
                    .toList();
            for (LocalDate snapshotDate : snapshotDates) {
                SnapshotDocKey snapshotDocKey = new TradingSnapshotDocKey(accountId, snapshotDate);
                List<String> oldStatementHeaderLines = dms.getStatementContentLinesIfExists(snapshotDocKey, 2);
                if (oldStatementHeaderLines != null) {
                    LocalDateTime oldStatementCreatedOn = parser.parseTradingStatementCreatedOn(oldStatementHeaderLines);
                    if (oldStatementCreatedOn.toLocalDate().isAfter(snapshotDate)) {
                        continue;
                    }
                    boolean oldIsStale = oldStatementCreatedOn.plus(staleTolerance).isBefore(now);
                    if (!oldIsStale) {
                        continue;
                    }
                }
                String fetchedContent = fetcher.fetchSnapshotStatement(account, snapshotDate);

                dms.putStatement(snapshotDocKey, fetchedContent);
                LOG.debug("getTradingPtfProgress - saved fetched snapshot statement - {}", snapshotDocKey);
            }
        }

        List<? extends SnapshotDocKey> snapshotDocKeys = dms.getSnapshotDocKeys(accountId, TRADING, fromDateIncl, toDateIncl);
        Currency pcy = account.ccy();
        List<DateAmount> navs = snapshotDocKeys
                .stream()
                .map(dms::getStatementContent)
                .map(parser::parseSnapshotStatement)
                .peek(s -> Assert.isTrue(s.nav().ccy() == pcy))
                .map(s -> new DateAmount(s.date(), s.nav().amount())).toList();
        return new PtfActivity(newFinTrans, navs);
    }

    private PtfActivity getSavingPtfProgress(
            Account account,
            LocalDate fromDateIncl,
            LocalDate toDateIncl,
            Duration staleTolerance,
            boolean online
    ) {
        LOG.debug("getSavingPtfProgress({}, {}-{}, staleTolerance={}, online={})", account, fromDateIncl, toDateIncl, staleTolerance, online);
        assert SAVING.name().equals(account.type());
        String accountId = account.externalId();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        online = online && account.credentials() != null;

        if (online) {
            Assert.notNull(staleTolerance, () -> "staleTolerance must not be null");
            LocalDate fetchFromDateIncl = fromDateIncl.withDayOfMonth(1);
            LocalDate fetchToDateIncl = minDate(today, toDateIncl);

            for (LocalDate iterFromDateIncl = fetchFromDateIncl;
                 !iterFromDateIncl.isAfter(fetchToDateIncl);
                 iterFromDateIncl = iterFromDateIncl.plusMonths(1)
            ) {
                LocalDate iterToDateIncl = minDate(fetchToDateIncl, iterFromDateIncl.plusMonths(1).minusDays(1));
                assert !iterFromDateIncl.isAfter(iterToDateIncl);

                TransDocKey docKey = new SavingTransDocKey(accountId, iterFromDateIncl, iterToDateIncl);
                LocalDateTime oldModifiedOn = dms.getStatementLastModifiedTimeIfExists(docKey);
                if (oldModifiedOn != null) {
                    boolean oldIsStale;
                    if (iterToDateIncl.isEqual(today)) {
                        oldIsStale = oldModifiedOn.plus(staleTolerance).isBefore(now);
                    } else {
                        oldIsStale = !oldModifiedOn.toLocalDate().isAfter(iterToDateIncl);
                    }
                    if (!oldIsStale) {
                        continue;
                    }
                }
                if (iterFromDateIncl != fetchFromDateIncl) {
                    ThreadUtil.sleep(ofSeconds(12));
                }

                String fetchedContent = fetcher.fetchTransStatement(account, iterFromDateIncl, iterToDateIncl);
                boolean useful = dms.putStatement(docKey, fetchedContent);
                if (useful) {
                    LOG.debug("getSavingPtfProgress - saved fetched trans statement - {}", docKey);
                } else {
                    LOG.debug("getSavingPtfProgress - fetched trans statement is useless - {}", docKey);
                }
            }
        }

        List<? extends FiobDocKey> transDocKeys = dms.getTransDocKeys(accountId, SAVING, fromDateIncl, toDateIncl);
        List<SavingTransStatement> transStatements = transDocKeys
                .stream()
                .map(dms::getStatementContent)
                .map(parser::parseSavingTransStatement)
                .toList();

        SavingTransStatement mergedTransStatement = statementMerger.mergeSavingTransStatements(transStatements).orElse(null);
        if (mergedTransStatement == null) {
            return null;
        }

        if (!accountId.equals(mergedTransStatement.accountId())) {
            throw new IllegalStateException("Given externalId=%s does not match foundAccountId=%s"
                    .formatted(accountId, mergedTransStatement.accountId()));
        }
        if (mergedTransStatement.periodFrom().isAfter(fromDateIncl)) {
            throw new IllegalStateException("No TransStatement available for externalId=%s, fromDateIncl=%s"
                    .formatted(accountId, fromDateIncl));
        }

        List<FinTransaction> finTrans = finTransactionMapper.mapTransactions(mergedTransStatement);
        List<FinTransaction> newFinTrans = finTrans
                .stream()
                .filter(t -> !t.date().isBefore(fromDateIncl) && !t.date().isAfter(toDateIncl))
                .sorted(comparing(FinTransaction::date))
                .toList();

        return new PtfActivity(newFinTrans, null);
    }

    private static final Logger LOG = LoggerFactory.getLogger(FiobPtfActivityProviderImpl.class);

}
