package com.brinvex.ptfactivity.core.api.domain.constraints.fintransaction;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.core.api.domain.Asset;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class SellConstraints extends FinTransactionConstraints {

    SellConstraints(FinTransaction finTransaction) {
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

    @Negative
    @Override
    public BigDecimal getQty() {
        return super.getQty();
    }

    @Positive
    @Override
    public BigDecimal getPrice() {
        return super.getPrice();
    }

    @NotNull
    @Override
    public Asset getAsset() {
        return super.getAsset();
    }

    @Positive
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

    @DecimalMax("0.05")
    @Override
    public BigDecimal getFee() {
        return super.getFee();
    }

}
