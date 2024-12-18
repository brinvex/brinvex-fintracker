package com.brinvex.ptfactivity.core.api.domain.constraints.fintransaction;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.core.api.domain.Asset;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType;
import com.brinvex.ptfactivity.core.api.domain.constraints.asset.AssetConstraints;
import com.brinvex.ptfactivity.core.api.general.Validatable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNullElse;

public abstract class FinTransactionConstraints implements Validatable {

    public static FinTransactionConstraints of(FinTransaction finTransaction) {
        FinTransactionType type = finTransaction.type();
        return switch (type) {
            case DEPOSIT -> new DepositConstraints(finTransaction);
            case WITHDRAWAL -> new WithdrawalConstraints(finTransaction);
            case BUY -> new BuyConstraints(finTransaction);
            case SELL -> new SellConstraints(finTransaction);
            case FX_BUY -> new FxBuyConstraints(finTransaction);
            case FX_SELL -> new FxSellConstraints(finTransaction);
            case DIVIDEND -> new DividendConstraints(finTransaction);
            case INTEREST -> new InterestConstraints(finTransaction);
            case FEE -> new FeeConstraints(finTransaction);
            case TAX -> new TaxConstraints(finTransaction);
            case TRANSFORMATION -> new TransformationConstraints(finTransaction);
            case OTHER_INTERNAL_FLOW -> new OtherInternalFlowConstraints(finTransaction);
        };
    }

    private final FinTransaction finTransaction;

    protected FinTransactionConstraints(FinTransaction finTransaction) {
        this.finTransaction = finTransaction;
    }

    @NotNull
    public FinTransactionType getType() {
        return finTransaction.type();
    }

    @NotNull
    public LocalDate getDate() {
        return finTransaction.date();
    }

    public Currency getCcy() {
        return finTransaction.ccy();
    }

    @NotNull
    public BigDecimal getNetValue() {
        return finTransaction.netValue();
    }

    @NotNull
    public BigDecimal getQty() {
        return finTransaction.qty();
    }

    @Positive
    public BigDecimal getPrice() {
        return finTransaction.price();
    }

    public Asset getAsset() {
        return finTransaction.asset();
    }

    @Valid
    public AssetConstraints getAssetConstraints() {
        Asset asset = finTransaction.asset();
        return asset == null ? null : AssetConstraints.of(asset);
    }

    @NotNull
    public BigDecimal getGrossValue() {
        return finTransaction.grossValue();
    }

    public BigDecimal getTax() {
        return finTransaction.tax();
    }

    @NotNull
    public BigDecimal getFee() {
        return finTransaction.fee();
    }

    public String getGroupId() {
        return finTransaction.groupId();
    }

    public String getExternalId() {
        return finTransaction.externalId();
    }

    public String getExternalType() {
        return finTransaction.externalType();
    }

    public String getExternalDetail() {
        return finTransaction.externalDetail();
    }

    @DecimalMax("0.0001")
    public BigDecimal getGrossValueCalcDeviation() {
        BigDecimal netValue = getNetValue();
        if (netValue == null) {
            return null;
        }
        BigDecimal grossValue = getGrossValue();
        if (grossValue == null) {
            return null;
        }
        BigDecimal fee = getFee();
        if (fee == null) {
            return null;
        }
        BigDecimal tax = requireNonNullElse(getTax(), BigDecimal.ZERO);

        BigDecimal calcGrossValue = netValue.subtract(fee).subtract(tax);
        return calcGrossValue.subtract(grossValue).abs();
    }

    @AssertTrue
    public boolean isAssetNotNullIfQtyIsNotZero() {
        BigDecimal qty = getQty();
        return (qty == null || qty.compareTo(BigDecimal.ZERO) == 0) || getAsset() != null;
    }

    @AssertTrue
    public boolean isCcyNotNullIfNetValueIsNotZero() {
        BigDecimal netValue = getNetValue();
        return netValue == null || netValue.compareTo(BigDecimal.ZERO) == 0 || getCcy() != null;
    }

    @AssertTrue
    public boolean isCcyNotNullIfGrossValueIsNotZero() {
        BigDecimal grossValue = getGrossValue();
        return grossValue == null || grossValue.compareTo(BigDecimal.ZERO) == 0 || getCcy() != null;
    }

    public boolean isNetValueNotZero() {
        BigDecimal netValue = getNetValue();
        return netValue != null && netValue.compareTo(BigDecimal.ZERO) != 0;
    }

    public boolean isFeeNotZero() {
        BigDecimal fee = getFee();
        return fee != null && fee.compareTo(BigDecimal.ZERO) != 0;
    }

    public boolean isTaxNotZero() {
        BigDecimal tax = getTax();
        return tax != null && tax.compareTo(BigDecimal.ZERO) != 0;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add(String.valueOf(finTransaction))
                .toString();
    }
}
