package com.brinvex.ptfactivity.connector.rvlt.api.model;

import java.time.LocalDate;
import java.util.Comparator;

import static java.util.Comparator.comparing;

public sealed interface RvltDocKey {

    String accountNumber();

    record TradingAccountStatementDocKey(
            @Override
            String accountNumber,
            LocalDate fromDateIncl,
            LocalDate toDateIncl
    ) implements RvltDocKey, Comparable<TradingAccountStatementDocKey> {

        private static final Comparator<TradingAccountStatementDocKey> COMPARATOR =
                comparing(TradingAccountStatementDocKey::accountNumber)
                        .thenComparing(TradingAccountStatementDocKey::fromDateIncl)
                        .thenComparing(TradingAccountStatementDocKey::toDateIncl);

        @Override
        public int compareTo(TradingAccountStatementDocKey other) {
            return COMPARATOR.compare(this, other);
        }
    }

    record PnlStatementDocKey(
            @Override
            String accountNumber,
            LocalDate fromDateIncl,
            LocalDate toDateIncl
    ) implements RvltDocKey, Comparable<PnlStatementDocKey> {

        private static final Comparator<PnlStatementDocKey> COMPARATOR =
                comparing(PnlStatementDocKey::accountNumber)
                        .thenComparing(PnlStatementDocKey::fromDateIncl)
                        .thenComparing(PnlStatementDocKey::toDateIncl);

        @Override
        public int compareTo(PnlStatementDocKey other) {
            return COMPARATOR.compare(this, other);
        }
    }

}
