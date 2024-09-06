package com.brinvex.fintracker.core.api.model.domain.constraints.fintransaction;

import com.brinvex.fintracker.core.api.model.domain.Asset;
import com.brinvex.fintracker.core.api.model.domain.FinTransaction;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class TransformationConstraints extends FinTransactionConstraints {

    TransformationConstraints(FinTransaction finTransaction) {
        super(finTransaction);
    }

    @NotNull
    @Override
    public Asset getAsset() {
        return super.getAsset();
    }

    @Min(0)
    @Max(0)
    @NotNull
    @Override
    public BigDecimal getTax() {
        return super.getTax();
    }

    @Min(0)
    @Max(0)
    @Override
    public BigDecimal getFee() {
        return super.getFee();
    }

}
