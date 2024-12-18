package com.brinvex.ptfactivity.connector.ibkr.api.model;

import java.time.LocalDate;
import java.util.Comparator;

import static java.util.Comparator.comparing;

public sealed interface IbkrDocKey {

    String accountId();

    record ActivityDocKey(
            @Override
            String accountId,
            LocalDate fromDateIncl,
            LocalDate toDateIncl
    ) implements IbkrDocKey, Comparable<ActivityDocKey> {

        private static final Comparator<ActivityDocKey> COMPARATOR =
                comparing(ActivityDocKey::accountId)
                        .thenComparing(ActivityDocKey::fromDateIncl)
                        .thenComparing(ActivityDocKey::toDateIncl);

        @Override
        public int compareTo(ActivityDocKey other) {
            return COMPARATOR.compare(this, other);
        }
    }

    record TradeConfirmDocKey(
            @Override
            String accountId,
            LocalDate date
    ) implements IbkrDocKey, Comparable<TradeConfirmDocKey> {

        private static final Comparator<TradeConfirmDocKey> COMPARATOR =
                comparing(TradeConfirmDocKey::accountId)
                        .thenComparing(TradeConfirmDocKey::date);

        @Override
        public int compareTo(TradeConfirmDocKey other) {
            return COMPARATOR.compare(this, other);
        }
    }

}
