package com.brinvex.fintracker.core.api.model.general;

import java.time.LocalDate;

import static java.util.Objects.requireNonNull;

public sealed interface DateRange {

    boolean contains(LocalDate date);

    record Inclusive(
            LocalDate left,
            LocalDate right
    ) implements DateRange {
        public Inclusive {
            requireNonNull(left);
            requireNonNull(right);
        }

        public Inclusive(String leftDate, String rightDate) {
            this(LocalDate.parse(leftDate), LocalDate.parse(rightDate));
        }

        @Override
        public boolean contains(LocalDate date) {
            return !date.isBefore(left) && !date.isAfter(right);
        }
    }

    record LeftInclusive(
            LocalDate left,
            LocalDate right
    ) implements DateRange {
        public LeftInclusive {
            requireNonNull(left);
        }

        public LeftInclusive(String leftDate, String rightDate) {
            this(LocalDate.parse(leftDate), rightDate == null ? null : LocalDate.parse(rightDate));
        }

        @Override
        public boolean contains(LocalDate date) {
            return !date.isBefore(left) && (right == null || date.isBefore(right));
        }
    }

    record RightInclusive(
            LocalDate left,
            LocalDate right
    ) implements DateRange {
        public RightInclusive {
            requireNonNull(right);
        }

        public RightInclusive(String leftDate, String rightDate) {
            this(leftDate == null ? null : LocalDate.parse(leftDate), LocalDate.parse(rightDate));
        }

        @Override
        public boolean contains(LocalDate date) {
            return (left == null || date.isAfter(left)) && !date.isAfter(right);
        }
    }

    record Exclusive(
            LocalDate left,
            LocalDate right
    ) implements DateRange {
        public Exclusive {
        }

        public Exclusive(String leftDate, String rightDate) {
            this(leftDate == null ? null : LocalDate.parse(leftDate), rightDate == null ? null : LocalDate.parse(rightDate));
        }

        @Override
        public boolean contains(LocalDate date) {
            return (left == null || date.isAfter(left)) && (right == null || date.isBefore(right));
        }
    }

}
