package com.brinvex.fintracker.common.impl.builder;

import com.brinvex.fintracker.api.model.domain.Asset;
import com.brinvex.fintracker.api.model.domain.FinTransaction;
import com.brinvex.fintracker.api.model.domain.FinTransactionType;
import com.brinvex.util.java.validation.Assert;
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
        Assert.notNull(type);
        Assert.notNull(date);
        Assert.notNull(netValue);
        Assert.notNull(qty);
        Assert.notNull(grossValue);
        Assert.notNullNotBlank(extraId);

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
