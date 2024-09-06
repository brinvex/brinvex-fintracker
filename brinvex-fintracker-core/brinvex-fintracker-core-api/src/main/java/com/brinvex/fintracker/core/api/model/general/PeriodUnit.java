package com.brinvex.fintracker.core.api.model.general;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.IsoFields;

import static java.util.Objects.requireNonNull;

public enum PeriodUnit {

    DAY(Period.ofDays(1)),

    MONTH(Period.ofMonths(1)),

    QUARTER(Period.ofMonths(3)),

    YEAR(Period.ofYears(1));

    private final Period period;

    PeriodUnit(Period period) {
        this.period = period;
    }

    public Period period() {
        return period;
    }

    public LocalDate startDate(LocalDate date) {
        return switch (this) {
            case DAY -> requireNonNull(date);
            case MONTH -> date.withDayOfMonth(1);
            case QUARTER -> date.with(IsoFields.DAY_OF_QUARTER, 1);
            case YEAR -> date.withDayOfYear(1);
        };
    }

}

