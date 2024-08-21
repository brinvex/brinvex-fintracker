package com.brinvex.fintracker.connector.ibkr.api.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;

public sealed interface IbkrDocKey extends Serializable {

    String accountId();

    record ActivityDocKey(
            @Override
            String accountId,
            LocalDate fromDayIncl,
            LocalDate toDayIncl
    ) implements IbkrDocKey, Comparable<ActivityDocKey>, Serializable {

        private static final Comparator<ActivityDocKey> COMPARATOR =
                Comparator.comparing(ActivityDocKey::accountId)
                        .thenComparing(ActivityDocKey::fromDayIncl)
                        .thenComparing(ActivityDocKey::toDayIncl);

        @Override
        public int compareTo(ActivityDocKey other) {
            return COMPARATOR.compare(this, other);
        }
    }

    record TradeConfirmDocKey(
            @Override
            String accountId,
            LocalDate day,
            LocalTime whenGenerated
    ) implements IbkrDocKey, Comparable<TradeConfirmDocKey>, Serializable {

        private static final Comparator<TradeConfirmDocKey> COMPARATOR =
                Comparator.comparing(TradeConfirmDocKey::accountId)
                        .thenComparing(TradeConfirmDocKey::day)
                        .thenComparing(TradeConfirmDocKey::whenGenerated);

        @Override
        public int compareTo(TradeConfirmDocKey other) {
            return COMPARATOR.compare(this, other);
        }
    }
}
