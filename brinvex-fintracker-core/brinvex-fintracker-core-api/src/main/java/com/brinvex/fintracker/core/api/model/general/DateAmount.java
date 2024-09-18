package com.brinvex.fintracker.core.api.model.general;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DateAmount(LocalDate date, BigDecimal amount) implements Serializable {

    public DateAmount {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null, amount=%s".formatted(amount));
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null, date=%s".formatted(date));
        }
    }

    public DateAmount add(DateAmount other) {
        if (!date.isEqual(other.date())) {
            throw new IllegalArgumentException(
                    "The dates of the two DateAmount instances must be the same to perform the add operation, given: %s, %s"
                            .formatted(this, other)
            );
        }
        return new DateAmount(date, amount.add(other.amount));
    }

    public boolean isBefore(LocalDate date) {
        return this.date.isBefore(date);
    }

    public boolean isAfter(LocalDate date) {
        return this.date.isAfter(date);
    }

}
