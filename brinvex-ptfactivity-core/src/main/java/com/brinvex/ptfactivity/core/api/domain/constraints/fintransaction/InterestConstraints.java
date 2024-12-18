package com.brinvex.ptfactivity.core.api.domain.constraints.fintransaction;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class InterestConstraints extends FinTransactionConstraints {

    InterestConstraints(FinTransaction finTransaction) {
        super(finTransaction);
    }

    @NotNull
    @Override
    public Currency getCcy() {
        return super.getCcy();
    }

    @Positive
    @Override
    public BigDecimal getNetValue() {
        return super.getNetValue();
    }

    @Min(0)
    @Max(0)
    @Override
    public BigDecimal getQty() {
        return super.getQty();
    }

    @Null
    @Override
    public BigDecimal getPrice() {
        return super.getPrice();
    }

    @Positive
    @Override
    public BigDecimal getGrossValue() {
        return super.getGrossValue();
    }

    @NegativeOrZero
    @Override
    public BigDecimal getFee() {
        return super.getFee();
    }

}
