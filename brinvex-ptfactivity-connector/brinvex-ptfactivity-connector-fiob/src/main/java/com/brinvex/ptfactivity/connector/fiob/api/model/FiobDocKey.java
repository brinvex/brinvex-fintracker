package com.brinvex.ptfactivity.connector.fiob.api.model;

import java.time.LocalDate;
import java.util.Comparator;

import static java.util.Comparator.comparing;

public sealed interface FiobDocKey {

    String accountId();

    sealed interface TransDocKey extends FiobDocKey {
        LocalDate fromDateIncl();
        LocalDate toDateIncl();
    }

    sealed interface SnapshotDocKey extends FiobDocKey {
        LocalDate date();
    }

    record TradingTransDocKey(
            @Override
            String accountId,
            @Override
            LocalDate fromDateIncl,
            @Override
            LocalDate toDateIncl
    ) implements TransDocKey, Comparable<TradingTransDocKey> {

        private static final Comparator<TradingTransDocKey> COMPARATOR =
                comparing(TradingTransDocKey::accountId)
                        .thenComparing(TradingTransDocKey::fromDateIncl)
                        .thenComparing(TradingTransDocKey::toDateIncl);

        @Override
        public int compareTo(TradingTransDocKey other) {
            return COMPARATOR.compare(this, other);
        }
    }

    record TradingSnapshotDocKey(
            @Override
            String accountId,
            @Override
            LocalDate date
    ) implements SnapshotDocKey, Comparable<TradingSnapshotDocKey> {

        private static final Comparator<TradingSnapshotDocKey> COMPARATOR =
                comparing(TradingSnapshotDocKey::accountId)
                        .thenComparing(TradingSnapshotDocKey::date);

        @Override
        public int compareTo(TradingSnapshotDocKey other) {
            return COMPARATOR.compare(this, other);
        }
    }

    record SavingTransDocKey(
            @Override
            String accountId,
            @Override
            LocalDate fromDateIncl,
            @Override
            LocalDate toDateIncl
    ) implements TransDocKey, Comparable<SavingTransDocKey> {

        private static final Comparator<SavingTransDocKey> COMPARATOR =
                comparing(SavingTransDocKey::accountId)
                        .thenComparing(SavingTransDocKey::fromDateIncl)
                        .thenComparing(SavingTransDocKey::toDateIncl);

        @Override
        public int compareTo(SavingTransDocKey other) {
            return COMPARATOR.compare(this, other);
        }
    }


}
