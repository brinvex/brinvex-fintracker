package com.brinvex.fintracker.api.model.domain.constraints.fintransaction;

import com.brinvex.fintracker.api.exception.NotYetImplementedException;
import com.brinvex.fintracker.api.model.domain.Asset;
import com.brinvex.fintracker.api.model.domain.FinTransaction;
import com.brinvex.fintracker.api.model.domain.FinTransactionType;
import com.brinvex.fintracker.api.model.domain.constraints.asset.AssetConstraints;
import com.brinvex.fintracker.api.model.general.Validatable;
import com.brinvex.fintracker.api.util.Regex;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
            case CASH_DIVIDEND -> new CashDividendConstraints(finTransaction);
            case INTEREST -> new InterestConstraints(finTransaction);
            case FEE -> new FeeConstraints(finTransaction);
            case TAX -> new TaxConstraints(finTransaction);
            case TRANSFORMATION -> new TransformationConstraints(finTransaction);
            case OTHER_INTERNAL_FLOW -> throw new NotYetImplementedException(finTransaction.toString());
        };
    }

    private final FinTransaction finTransaction;

    protected FinTransactionConstraints(FinTransaction finTransaction) {
        this.finTransaction = finTransaction;
    }

    public String getId() {
        return finTransaction.id();
    }

    @NotNull
    public FinTransactionType getType() {
        return finTransaction.type();
    }

    @NotNull
    public LocalDate getDate() {
        return finTransaction.date();
    }

    @Pattern(regexp = Regex.CCY)
    public String getCcy() {
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

    public LocalDate getSettleDate() {
        return finTransaction.settleDate();
    }

    public String getGroupId() {
        return finTransaction.groupId();
    }

    public String getExtraId() {
        return finTransaction.extraId();
    }

    public String getExtraType() {
        return finTransaction.extraType();
    }

    public String getExtraDetail() {
        return finTransaction.extraDetail();
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
