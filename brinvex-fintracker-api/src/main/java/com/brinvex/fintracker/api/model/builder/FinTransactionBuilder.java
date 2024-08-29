package com.brinvex.fintracker.api.model.builder;

import com.brinvex.fintracker.api.model.domain.Asset;
import com.brinvex.fintracker.api.model.domain.FinTransaction;
import com.brinvex.fintracker.api.model.domain.FinTransactionType;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Accessors(fluent = true, chain = true)
public class FinTransactionBuilder {
    private String id;
    private FinTransactionType type;
    private LocalDate date;
    private String ccy;
    private BigDecimal netValue;

    private BigDecimal qty;
    private BigDecimal price;
    private Asset asset;

    private BigDecimal grossValue;
    private BigDecimal tax;
    private BigDecimal fee;

    private LocalDate settleDate;
    private String groupId;
    private String extraId;
    private String extraType;
    private String extraDetail;

    public FinTransaction build() {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (date == null) {
            throw new IllegalArgumentException("date cannot be null");
        }
        if (netValue == null) {
            throw new IllegalArgumentException("netValue cannot be null");
        }
        if (qty == null) {
            throw new IllegalArgumentException("qty cannot be null");
        }
        if (grossValue == null) {
            throw new IllegalArgumentException("grossValue cannot be null");
        }
        if (extraId == null || extraId.isBlank()) {
            throw new IllegalArgumentException("extraId cannot be null or blank");
        }

        return new FinTransaction(
                id,
                type,
                date,
                ccy,
                netValue,
                qty,
                price,
                asset,
                grossValue,
                tax,
                fee,
                settleDate,
                groupId,
                extraId,
                extraType,
                extraDetail
        );
    }
}
