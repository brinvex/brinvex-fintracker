package com.brinvex.fintracker.connector.ibkr.api.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;

public record IbkrAccount(
        LocalDate validFromIncl,
        LocalDate validToIncl,
        IdAccount actualIdAccount,
        List<IdAccount> oldIdAccounts
) {

    /**
     * On August 1, 2024, Interactive Brokers Ireland Limited (IBIE) and
     * Interactive Brokers Central Europe Zrt. (IBCE) merged into a single entity,
     * with all former IBCE clients now serviced by IBIE.
     * As a result, former IBCE clients got new Account IDs under IBIE.
     */
    public record IdAccount(
            String accountId,
            Credentials credentials,
            LocalDate idValidFromIncl,
            LocalDate idValidToIncl
    ) {
        public IdAccount {
            if (accountId == null || accountId.isBlank()) {
                throw new IllegalArgumentException("accountId must not be null or blank");
            }
            if (idValidFromIncl == null) {
                throw new IllegalArgumentException("idValidFromIncl must not be null");
            }
            if (idValidToIncl != null && idValidToIncl.isBefore(idValidFromIncl)) {
                throw new IllegalArgumentException("idValidToIncl must not be before idValidFromIncl, given: %s %s".formatted(idValidToIncl, idValidFromIncl));
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
                throw new IllegalArgumentException("activityFlexQueryId must not be null");
            }
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Credentials.class.getSimpleName() + "[", "]")
                    .add("token=%s***".formatted(token == null ? null : token.substring(0, 2)))
                    .add("actFlexQueryId=%s***".formatted(activityFlexQueryId == null ? null : activityFlexQueryId.substring(0, 3)))
                    .add("tcFlexQueryId=%s***".formatted(tradeConfirmFlexQueryId == null ? null : tradeConfirmFlexQueryId.substring(0, 3)))
                    .toString();
        }
    }

    public IbkrAccount {
        if (validFromIncl == null) {
            throw new IllegalArgumentException("idValidFromIncl must not be null");
        }
        if (validToIncl != null && validFromIncl.isAfter(validToIncl)) {
            throw new IllegalArgumentException("idValidFromIncl must not be after idValidToIncl, given: %s, %s".formatted(validFromIncl, validToIncl));
        }
        if (actualIdAccount == null) {
            throw new IllegalArgumentException("actualIdAccount must not be null");
        }
        if (actualIdAccount.idValidFromIncl.isBefore(validFromIncl)) {
            throw new IllegalArgumentException("actualIdAccount.idValidFromIncl must not be before validFromIncl, given: %s, %s".formatted(actualIdAccount.idValidFromIncl, validFromIncl));
        }
        if ((actualIdAccount.idValidToIncl != null && validToIncl == null)
            || actualIdAccount.idValidToIncl == null && validToIncl != null
            || validToIncl != null && actualIdAccount.idValidToIncl.isEqual(validToIncl)
        ) {
            throw new IllegalArgumentException("actualIdAccount.idValidToIncl must be consistent with the validToIncl, given: %s, %s".formatted(actualIdAccount.idValidToIncl, validToIncl));
        }
        if (oldIdAccounts == null) {
            oldIdAccounts = emptyList();
        } else {
            for (IdAccount oldIdAccount : oldIdAccounts) {
                LocalDate oldIdValidFromIncl = oldIdAccount.idValidFromIncl;
                LocalDate oldIdValidToIncl = oldIdAccount.idValidToIncl;
                if (oldIdValidFromIncl.isBefore(validFromIncl)) {
                    throw new IllegalArgumentException("oldIdValidFromIncl must not be before validFromIncl, given: %s, %s".formatted(oldIdValidFromIncl, validFromIncl));
                }
                if (oldIdValidToIncl == null) {
                    throw new IllegalArgumentException("oldIdValidToIncl must not be null");
                }
                if (validToIncl != null && oldIdValidToIncl.isAfter(validToIncl)) {
                    throw new IllegalArgumentException("oldIdValidToIncl must be consistent with the validToIncl, given: %s, %s".formatted(oldIdValidToIncl, validToIncl));
                }
            }
        }
    }

    public static IbkrAccount of(Map<String, String> props) {
        String accountId = props.get("accountId");
        if (accountId == null || accountId.isEmpty()) {
            return null;
        }
        String token = props.get("token");
        String validFromInclStr = props.get("validFromIncl");
        String validToInclStr = props.get("validToIncl");
        String idValidFromInclStr = props.get("idValidFromIncl");
        String idValidToInclStr = props.get("idValidToIncl");
        LocalDate validFromIncl = validFromInclStr == null || validFromInclStr.isEmpty() ? null : LocalDate.parse(validFromInclStr);
        LocalDate validToIncl = validToInclStr == null || validToInclStr.isEmpty() ? null : LocalDate.parse(validToInclStr);
        LocalDate idValidFromIncl = idValidFromInclStr == null || idValidFromInclStr.isEmpty() ? validFromIncl : LocalDate.parse(idValidFromInclStr);
        LocalDate idValidToIncl = idValidToInclStr == null || idValidToInclStr.isEmpty() ? validToIncl : LocalDate.parse(idValidToInclStr);

        IbkrAccount.Credentials credentials;
        if (token != null && !token.isEmpty()) {
            String activityFlexQueryId = props.get("activityFlexQueryId");
            String tradeConfirmationFlexQueryId = props.get("tradeConfirmFlexQueryId");
            credentials = new IbkrAccount.Credentials(token, activityFlexQueryId, tradeConfirmationFlexQueryId);
        } else {
            credentials = null;
        }
        List<IdAccount> oldAccounts = new ArrayList<>();
        for (int oldIdx = 1; ; oldIdx++) {
            String oldAccountId = props.get("old%s.accountId".formatted(oldIdx));
            if (oldAccountId == null || oldAccountId.isEmpty()) {
                break;
            }
            String oldToken = props.get("old%s.token".formatted(oldIdx));
            String oldValidFromInclStr = props.get("old%s.idValidFromIncl".formatted(oldIdx));
            String oldValidToInclStr = props.get("old%s.idValidToIncl".formatted(oldIdx));
            LocalDate oldIdValidFromIncl = oldValidFromInclStr == null || oldValidToInclStr.isEmpty() ? null : LocalDate.parse(oldValidFromInclStr);
            LocalDate oldIdValidToIncl = oldValidToInclStr == null || oldValidToInclStr.isEmpty() ? null : LocalDate.parse(oldValidToInclStr);
            Credentials oldCredentials;
            if (oldToken != null && !oldToken.isEmpty()) {
                String oldActivityFlexQueryId = props.get("old%s.activityFlexQueryId".formatted(oldIdx));
                String oldTradeConfirmFlexQueryId = props.get("old%s.tradeConfirmFlexQueryId".formatted(oldIdx));
                oldCredentials = new Credentials(oldToken, oldActivityFlexQueryId, oldTradeConfirmFlexQueryId);
            } else {
                oldCredentials = null;
            }
            IdAccount oldAccount = new IdAccount(
                    oldAccountId,
                    oldCredentials,
                    oldIdValidFromIncl,
                    oldIdValidToIncl
            );
            oldAccounts.add(oldAccount);
        }
        oldAccounts.sort(comparing(IdAccount::idValidFromIncl).thenComparing(IdAccount::idValidToIncl));

        return new IbkrAccount(
                validFromIncl,
                validToIncl,
                new IdAccount(accountId, credentials, idValidFromIncl, idValidToIncl),
                oldAccounts
        );
    }

    public List<IdAccount> allIdAccountsAsc() {
        ArrayList<IdAccount> all = new ArrayList<>(oldIdAccounts);
        all.add(actualIdAccount);
        return all;
    }

    public List<IdAccount> allIdAccountsDesc() {
        ArrayList<IdAccount> all = new ArrayList<>();
        all.add(actualIdAccount);
        all.addAll(oldIdAccounts.reversed());
        return all;
    }

    public String accountId() {
        return actualIdAccount.accountId;
    }

    public Credentials credentials() {
        return actualIdAccount.credentials;
    }
}
