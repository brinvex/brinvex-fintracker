package com.brinvex.fintracker.api.model.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

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
}
