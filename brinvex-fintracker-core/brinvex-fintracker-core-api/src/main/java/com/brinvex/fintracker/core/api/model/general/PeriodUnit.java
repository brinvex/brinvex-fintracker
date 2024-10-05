package com.brinvex.fintracker.core.api.model.general;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;

import static java.util.Objects.requireNonNull;

public enum PeriodUnit implements Comparable<PeriodUnit> {

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

    public LocalDate adjStartDateIncl(LocalDate date) {
        return switch (this) {
            case DAY -> requireNonNull(date);
            case MONTH -> date.withDayOfMonth(1);
            case QUARTER -> date.with(IsoFields.DAY_OF_QUARTER, 1);
            case YEAR -> date.withDayOfYear(1);
        };
    }

    public LocalDate adjEndDateIncl(LocalDate date) {
        return switch (this) {
            case DAY -> requireNonNull(date);
            case MONTH -> date.withDayOfMonth(1).plusMonths(1).minusDays(1);
            case QUARTER -> date.with(IsoFields.DAY_OF_QUARTER, 1).plusMonths(3).minusDays(1);
            case YEAR -> date.withDayOfYear(1).plusYears(1).minusDays(1);
        };
    }

    public String caption(LocalDate dateIncl) {
        return switch (this) {
            case DAY -> Lazy.DAY_CAPTION_FMT.format(dateIncl);
            case MONTH -> Lazy.MONTH_CAPTION_FMT.format(dateIncl);
            case QUARTER -> String.valueOf(dateIncl.get(IsoFields.QUARTER_OF_YEAR));
            case YEAR -> String.valueOf(dateIncl.getYear());
        };
    }

    public int frequencyPerYear() {
        return switch (this) {
            case DAY -> throw new UnsupportedOperationException(
                    "frequencyPerYear is undefined for PeriodUnit.DAY because the number of days per year is not always the same"
            );
            case MONTH -> 12;
            case QUARTER -> 4;
            case YEAR -> 1;
        };
    }

    private static class Lazy {
        private static final DateTimeFormatter DAY_CAPTION_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private static final DateTimeFormatter MONTH_CAPTION_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    }
}

