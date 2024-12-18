package com.brinvex.ptfactivity.core.api.domain.constraints.fintransaction;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.core.api.domain.Asset;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

import java.math.BigDecimal;

public class DividendConstraints extends FinTransactionConstraints {

    DividendConstraints(FinTransaction finTransaction) {
        super(finTransaction);
    }

    @NotNull
    @Override
    public Currency getCcy() {
        return super.getCcy();
    }

    @NotNull
    @Override
    public BigDecimal getNetValue() {
        return super.getNetValue();
    }

    @NotNull
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

    @NotNull
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
