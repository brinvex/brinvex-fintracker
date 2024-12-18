package com.brinvex.ptfactivity.connector.ibkr.api.model;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.core.api.domain.Account;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;

/**
 * On August 1, 2024, Interactive Brokers Ireland Limited (IBIE) and
 * Interactive Brokers Central Europe Zrt. (IBCE) merged into a single entity,
 * with all former IBCE clients now serviced by IBIE.
 * As a result, former IBCE clients got new Account IDs under IBIE.
 */
public record IbkrAccount(
        String externalId,
        Currency ccy,
        LocalDate openDate,
        LocalDate closeDate,
        Credentials credentials,
        LocalDate externalIdValidFromIncl,
        List<MigratedAccount> migratedAccounts
) {

    public record MigratedAccount(
            String externalId,
            Credentials credentials,
            LocalDate externalIdValidFromIncl,
            LocalDate externalIdValidToIncl
    ) {
        public MigratedAccount {
            if (externalId == null || externalId.isBlank()) {
                throw new IllegalArgumentException("externalId must not be null or blank");
            }
            if (externalIdValidFromIncl == null) {
                throw new IllegalArgumentException("externalIdValidFromIncl must not be null");
            }
            if (externalIdValidToIncl == null) {
                throw new IllegalArgumentException("externalIdValidToIncl must not be null");
            }
            if (externalIdValidToIncl.isBefore(externalIdValidFromIncl)) {
                throw new IllegalArgumentException("externalIdValidToIncl must not be before externalIdValidFromIncl, given: %s %s".formatted(externalIdValidToIncl, externalIdValidFromIncl));
            }
        }
    }

    public record Credentials(
            String token,
            String activityFlexQueryId,
            String tradeConfirmFlexQueryId
    ) {
        public Credentials {
            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException("token must not be null or blank");
            }
            if (activityFlexQueryId == null || activityFlexQueryId.isBlank()) {
                throw new IllegalArgumentException("activityFlexQueryId must not be null or blank");
            }
            if (tradeConfirmFlexQueryId != null && tradeConfirmFlexQueryId.isBlank()) {
                tradeConfirmFlexQueryId = null;
            }
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", "Credentials[", "]")
                    .add("actFlexQueryId=***%s".formatted(activityFlexQueryId == null ? null : activityFlexQueryId.substring(activityFlexQueryId.length() - 2)))
                    .add("tcFlexQueryId=***%s".formatted(tradeConfirmFlexQueryId == null ? null : tradeConfirmFlexQueryId.substring(tradeConfirmFlexQueryId.length() - 2)))
                    .toString();
        }
    }

    public IbkrAccount {
        if (externalId == null) {
            throw new IllegalArgumentException("externalId must not be null");
        }
        if (ccy == null) {
            throw new IllegalArgumentException("ccy must not be null");
        }
        if (openDate == null) {
            throw new IllegalArgumentException("openDate must not be null");
        }
        if (closeDate != null && openDate.isAfter(closeDate)) {
            throw new IllegalArgumentException("openDate must not be after closeDate, given: %s, %s".formatted(openDate, closeDate));
        }
        if (externalIdValidFromIncl == null) {
            externalIdValidFromIncl = openDate;
        } else {
            if (externalIdValidFromIncl.isBefore(openDate) || (closeDate != null && externalIdValidFromIncl.isAfter(closeDate))) {
                throw new IllegalArgumentException("externalIdValidFromIncl must be between openDate and closeDate, given: %s, %s, %s"
                        .formatted(externalIdValidFromIncl, openDate, closeDate));
            }
        }
        if (migratedAccounts == null) {
            migratedAccounts = emptyList();
        } else {
            for (MigratedAccount migratedAccount : migratedAccounts) {
                if (migratedAccount.externalIdValidFromIncl.isBefore(openDate)) {
                    throw new IllegalArgumentException("migratedAccount.externalIdValidFromIncl must not be before openDate, given: %s, %s"
                            .formatted(migratedAccount.externalIdValidFromIncl, openDate));
                }
                if (closeDate != null && migratedAccount.externalIdValidToIncl.isAfter(closeDate)) {
                    throw new IllegalArgumentException("migratedAccount.externalIdValidToIncl must not be after closeDate, given: %s, %s"
                            .formatted(migratedAccount.externalIdValidToIncl, closeDate));
                }
            }
        }
    }

    public static IbkrAccount of(Account account) {
        Map<String, String> extraProps = new LinkedHashMap<>(account.extraProps());
        LocalDate idValidFrom = ofNullable(extraProps.remove("externalIdValidFromIncl")).filter(not(String::isBlank)).map(LocalDate::parse).orElse(null);
        String actFlexQueryId = ofNullable(extraProps.remove("activityFlexQueryId")).filter(not(String::isBlank)).orElse(null);
        String tcFlexQueryId = ofNullable(extraProps.remove("tradeConfirmFlexQueryId")).filter(not(String::isBlank)).orElse(null);
        Credentials credentials = account.credentials() == null ? null : new Credentials(account.credentials(), actFlexQueryId, tcFlexQueryId);

        List<MigratedAccount> migrAccounts = new ArrayList<>();
        for (int migrIdx = 1; ; migrIdx++) {
            String migratedId = extraProps.remove("migrated%s.externalId".formatted(migrIdx));
            if (migratedId == null || migratedId.isEmpty()) {
                break;
            }
            String migrToken = extraProps.remove("migrated%s.credentials".formatted(migrIdx));
            LocalDate migrIdValidFromIncl = ofNullable(extraProps.remove("migrated%s.externalIdValidFromIncl".formatted(migrIdx))).filter(not(String::isBlank)).map(LocalDate::parse).orElse(null);
            LocalDate migrIdValidToIncl = ofNullable(extraProps.remove("migrated%s.externalIdValidToIncl".formatted(migrIdx))).filter(not(String::isBlank)).map(LocalDate::parse).orElse(null);
            Credentials migrCredentials;
            if (migrToken != null && !migrToken.isEmpty()) {
                String migrActivityFlexQueryId = ofNullable(extraProps.remove("migrated%s.activityFlexQueryId".formatted(migrIdx))).filter(not(String::isBlank)).orElse(null);
                String migrTradeConfirmFlexQueryId = ofNullable(extraProps.remove("migrated%s.tradeConfirmFlexQueryId".formatted(migrIdx))).filter(not(String::isBlank)).orElse(null);
                migrCredentials = new Credentials(migrToken, migrActivityFlexQueryId, migrTradeConfirmFlexQueryId);
            } else {
                migrCredentials = null;
            }
            MigratedAccount oldAccount = new MigratedAccount(
                    migratedId,
                    migrCredentials,
                    migrIdValidFromIncl,
                    migrIdValidToIncl
            );
            migrAccounts.add(oldAccount);
        }
        List<MigratedAccount> sortedMigrAccounts = migrAccounts.stream()
                .sorted(comparing(MigratedAccount::externalIdValidFromIncl).thenComparing(MigratedAccount::externalIdValidToIncl))
                .toList();
        if (!migrAccounts.equals(sortedMigrAccounts)) {
            throw new IllegalArgumentException("migratedAccounts must be sorted");
        }
        if (!extraProps.isEmpty()) {
            throw new IllegalArgumentException("Unexpected extraProps: %s".formatted(extraProps.keySet()));
        }

        return new IbkrAccount(
                account.externalId(),
                account.ccy(),
                account.openDate(),
                account.closeDate(),
                credentials,
                idValidFrom,
                migrAccounts
        );
    }

    public static IbkrAccount of(Map<String, String> props) {
        Account account = Account.of(props);
        return account == null ? null : IbkrAccount.of(account);
    }

    public Account toBaseAccount() {
        LinkedHashMap<String, String> extraProps = new LinkedHashMap<>();
        for (int migrIdx = 1, migratedAccountsSize = migratedAccounts.size(); migrIdx <= migratedAccountsSize; migrIdx++) {
            MigratedAccount migrAccount = migratedAccounts.get(migrIdx - 1);
            extraProps.put("migrated%s.externalId".formatted(migrIdx), migrAccount.externalId());
            extraProps.put("migrated%s.externalIdValidFromIncl".formatted(migrIdx), migrAccount.externalIdValidFromIncl().toString());
            extraProps.put("migrated%s.externalIdValidToIncl".formatted(migrIdx), migrAccount.externalIdValidToIncl().toString());
            Credentials migrCredentials = migrAccount.credentials;
            if (migrCredentials != null) {
                extraProps.put("migrated%s.credentials".formatted(migrIdx), migrCredentials.token);
                extraProps.put("migrated%s.activityFlexQueryId".formatted(migrIdx), migrCredentials.activityFlexQueryId);
                if (migrCredentials.tradeConfirmFlexQueryId != null) {
                    extraProps.put("migrated%s.tradeConfirmFlexQueryId".formatted(migrIdx), migrCredentials.tradeConfirmFlexQueryId);
                }
            }
        }
        return new Account(
                null,
                null,
                ccy,
                openDate,
                closeDate,
                externalId,
                credentials == null ? null : credentials.token,
                unmodifiableMap(extraProps)
        );
    }
}
