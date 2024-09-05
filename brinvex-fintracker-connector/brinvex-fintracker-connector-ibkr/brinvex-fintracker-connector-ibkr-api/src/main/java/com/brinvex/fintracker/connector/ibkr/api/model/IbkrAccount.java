package com.brinvex.fintracker.connector.ibkr.api.model;

import java.time.LocalDate;
import java.util.Map;
import java.util.StringJoiner;

import static java.util.stream.Collectors.toMap;

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
     * To manage such transitions, we utilize the recursive structure IbkrAccount.Migration.
     */
    public record MigratedAccount(
            IbkrAccount oldAccount,
            LocalDate migrationFromIncl,
            LocalDate migrationToIncl
    ) {
        public MigratedAccount {
            if (oldAccount == null) {
                throw new IllegalArgumentException("oldAccount must not be null");
            }
            if (migrationFromIncl == null) {
                throw new IllegalArgumentException("migrationFromIncl must not be null");
            }
            if (migrationToIncl == null) {
                throw new IllegalArgumentException("migrationToIncl must not be null");
            }
            if (migrationFromIncl.isAfter(migrationToIncl)) {
                throw new IllegalArgumentException("migrationFromIncl must not be after migrationToIncl, %s, %s"
                        .formatted(migrationFromIncl, migrationToIncl));
            }
            MigratedAccount prevMigratedAccount = oldAccount.migratedAccount();
            if (prevMigratedAccount != null) {
                if (!prevMigratedAccount.migrationToIncl().isBefore(migrationFromIncl)) {
                    throw new IllegalArgumentException("recursive prev migrationToIncl must be before migrationFromIncl, %s, %s".formatted(
                            prevMigratedAccount, this
                    ));
                }
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
        String migrationKeyPartPrefix = "migratedAccount.";
        int migrationKeyPartLength = migrationKeyPartPrefix.length();
        Map<String, String> migrationProps = props.entrySet().stream().filter(e -> e.getKey()
                        .startsWith(migrationKeyPartPrefix))
                .collect(toMap(e -> e.getKey().substring(migrationKeyPartLength), Map.Entry::getValue));

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
            MigratedAccount migratedAccount;
            if (!migrationProps.isEmpty()) {
                LocalDate migrationFromIncl = LocalDate.parse(migrationProps.get("migrationFromIncl"));
                LocalDate migrationToIncl = LocalDate.parse(migrationProps.get("migrationToIncl"));
                migratedAccount = new MigratedAccount(IbkrAccount.of(migrationProps), migrationFromIncl, migrationToIncl);
            } else {
                migratedAccount = null;
            }
            account = new IbkrAccount(accountId, credentials, migratedAccount);
        }
        return account;
    }

}
