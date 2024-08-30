package com.brinvex.fintracker.api.model.domain.constraints.fintransaction;

import com.brinvex.fintracker.api.model.domain.FinTransaction;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FeeConstraints extends FinTransactionConstraints {

    FeeConstraints(FinTransaction finTransaction) {
        super(finTransaction);
    }

    @NotNull
    @Override
    public String getCcy() {
        return super.getCcy();
    }

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

    @Min(0)
    @Max(0)
    @Override
    public BigDecimal getGrossValue() {
        return super.getGrossValue();
    }

    @Min(0)
    @Max(0)
    @NotNull
    @Override
    public BigDecimal getTax() {
        return super.getTax();
    }

    @NotNull
    @Override
    public LocalDate getSettleDate() {
        return super.getSettleDate();
    }

    @AssertTrue
    @Override
    public boolean isNetValueNotZero() {
        return super.isNetValueNotZero();
    }

    @AssertTrue
    @Override
    public boolean isFeeNotZero() {
        return super.isFeeNotZero();
    }

}
