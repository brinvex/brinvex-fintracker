package com.brinvex.fintracker.core.api.model.general;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

import static java.util.Comparator.comparing;

public record DateAmount(LocalDate date, BigDecimal amount) implements Comparable<DateAmount>, Serializable {

    private static final Comparator<DateAmount> COMPARATOR = comparing(DateAmount::date);

    @Override
    public int compareTo(DateAmount other) {
        return COMPARATOR.compare(this, other);
    }
}
