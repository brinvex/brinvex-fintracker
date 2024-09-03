package com.brinvex.fintracker.connector.ibkr.api.model;

import java.time.LocalDate;
import java.util.Map;
import java.util.StringJoiner;

public record IbkrAccount(
        String accountId,
        Credentials credentials,
        MigratedAccount migratedAccount
) {

    public record Credentials(
            String token,
            String activityFlexQueryId,
            String tradeConfirmFlexQueryId
    ) {

        public Credentials {
            if (token == null) {
                throw new IllegalArgumentException("token must not be null");
            }
            if (activityFlexQueryId == null) {
                throw new IllegalArgumentException("activityFlexQueryId must not be null");
            }
            if (tradeConfirmFlexQueryId == null) {
                throw new IllegalArgumentException("tradeConfirmFlexQueryId must not be null");
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

    /**
     * On August 1, 2024, Interactive Brokers Ireland Limited (IBIE) and
     * Interactive Brokers Central Europe Zrt. (IBCE) merged into a single entity,
     * with all former IBCE clients now serviced by IBIE.
     * As a result, former IBCE clients will be assigned new Account IDs under IBIE.
     * To manage this transition, we utilize the IbkrAccount.migratedAccount structure
     * to record the old Account ID and the date of migration.
     */
    public record MigratedAccount(
            String oldAccountId,
            LocalDate oldAccountValidToIncl
    ) {
        public MigratedAccount {
            if (oldAccountId == null) {
                throw new IllegalArgumentException("oldAccountId must not be null");
            }
            if (oldAccountValidToIncl == null) {
                throw new IllegalArgumentException("oldAccountValidToIncl must not be null");
            }
        }
    }

    public IbkrAccount(String accountId) {
        this(accountId, null, null);
    }

    public IbkrAccount(String accountId, Credentials credentials) {
        this(accountId, credentials, null);
    }

    public IbkrAccount(String accountId, MigratedAccount migratedAccount) {
        this(accountId, null, migratedAccount);
    }

    public IbkrAccount {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId must not be null or blank");
        }
    }

    public static IbkrAccount of(Map<String, String> props) {
        String accountId = props.get("accountId");
        String token = props.get("token");
        String activityFlexQueryId = props.get("activityFlexQueryId");
        String tradeConfirmationFlexQueryId = props.get("tradeConfirmFlexQueryId");
        String oldAccountId = props.get("oldAccountId");
        String oldAccountValidToIncl = props.get("oldAccountValidToIncl");

        IbkrAccount account;
        if (accountId == null || accountId.isEmpty()) {
            account = null;
        } else {
            IbkrAccount.Credentials credentials;
            if (token != null && !token.isBlank()) {
                credentials = new IbkrAccount.Credentials(token, activityFlexQueryId, tradeConfirmationFlexQueryId);
            } else {
                credentials = null;
            }
            IbkrAccount.MigratedAccount migratedAccount;
            if (oldAccountId != null && !oldAccountId.isBlank()) {
                migratedAccount = new IbkrAccount.MigratedAccount(oldAccountId, LocalDate.parse(oldAccountValidToIncl));
            } else {
                migratedAccount = null;
            }
            account = new IbkrAccount(accountId, credentials, migratedAccount);
        }
        return account;
    }

}
