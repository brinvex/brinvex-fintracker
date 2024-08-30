package com.brinvex.fintracker.api.model.domain;

import lombok.Builder;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import static java.util.Objects.requireNonNull;

@Builder
@Accessors(fluent = true, chain = true)
public record FinTransaction(
        String id,
        FinTransactionType type,
        LocalDate date,
        String ccy,
        BigDecimal netValue,
        BigDecimal qty,
        BigDecimal price,
        Asset asset,
        BigDecimal grossValue,
        BigDecimal tax,
        BigDecimal fee,
        LocalDate settleDate,
        String groupId,
        String extraId,
        String extraType,
        String extraDetail
) implements Serializable {

    public FinTransaction {
        requireNonNull(type);
    }
}
