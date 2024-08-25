package com.brinvex.fintracker.api.model.general;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

import static java.util.Comparator.comparing;

public record DayAmount(LocalDate day, BigDecimal amount) implements Comparable<DayAmount>, Serializable {

    private static final Comparator<DayAmount> COMPARATOR = comparing(DayAmount::day);

    @Override
    public int compareTo(DayAmount other) {
        return COMPARATOR.compare(this, other);
    }
}
