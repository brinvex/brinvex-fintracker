package com.brinvex.ptfactivity.core.api.domain.constraints.fintransaction;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.core.api.domain.Asset;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

import java.math.BigDecimal;

public class TaxConstraints extends FinTransactionConstraints {

    TaxConstraints(FinTransaction finTransaction) {
        super(finTransaction);
    }

    @NotNull
    @Override
    public Currency getCcy() {
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

    @NotNull
    @Override
    public Asset getAsset() {
        return super.getAsset();
    }

    @Min(0)
    @Max(0)
    @Override
    public BigDecimal getGrossValue() {
        return super.getGrossValue();
    }

    @NotNull
    @Override
    public BigDecimal getTax() {
        return super.getTax();
    }

    @AssertTrue
    @Override
    public boolean isNetValueNotZero() {
        return super.isNetValueNotZero();
    }

    @AssertTrue
    @Override
    public boolean isTaxNotZero() {
        return super.isTaxNotZero();
    }

}
