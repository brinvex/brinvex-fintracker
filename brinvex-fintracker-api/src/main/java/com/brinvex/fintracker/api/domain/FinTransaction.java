package com.brinvex.fintracker.api.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FinTransaction(
        String id,
        FinTransactionType type,
        LocalDateTime dateTime,
        String ccy,
        BigDecimal netValue,

        BigDecimal qty,
        BigDecimal price,
        Asset asset,

        BigDecimal grossValue,
        BigDecimal tax,
        BigDecimal fee,

        String groupId,
        String extraId,
        String extraType,
        String extraDetail
) implements Serializable {
}
