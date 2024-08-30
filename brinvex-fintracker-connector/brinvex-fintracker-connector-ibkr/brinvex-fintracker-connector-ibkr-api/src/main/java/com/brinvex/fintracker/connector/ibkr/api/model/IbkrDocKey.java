package com.brinvex.fintracker.connector.ibkr.api.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;

import static java.util.Comparator.comparing;

public sealed interface IbkrDocKey extends Serializable {

    String accountId();

    record ActivityDocKey(
            @Override
            String accountId,
            LocalDate fromDateIncl,
            LocalDate toDateIncl
    ) implements IbkrDocKey, Comparable<ActivityDocKey>, Serializable {

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
            LocalDate date,
            LocalTime whenGenerated
    ) implements IbkrDocKey, Comparable<TradeConfirmDocKey>, Serializable {

        private static final Comparator<TradeConfirmDocKey> COMPARATOR =
                comparing(TradeConfirmDocKey::accountId)
                        .thenComparing(TradeConfirmDocKey::date)
                        .thenComparing(TradeConfirmDocKey::whenGenerated);

        @Override
        public int compareTo(TradeConfirmDocKey other) {
            return COMPARATOR.compare(this, other);
        }
    }

}
