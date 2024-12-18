package com.brinvex.ptfactivity.core.api.domain;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static java.math.BigDecimal.ZERO;
import static java.util.Objects.requireNonNullElse;

public record FinTransaction(
        FinTransactionType type,
        LocalDate date,
        Currency ccy,
        BigDecimal netValue,
        BigDecimal qty,
        BigDecimal price,
        Asset asset,
        BigDecimal grossValue,
        BigDecimal tax,
        BigDecimal fee,
        LocalDate settleDate,
        String groupId,
        String externalId,
        String externalType,
        String externalDetail
) {

    public FinTransaction {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
    }

    public static FinTransactionBuilder builder() {
        return new FinTransactionBuilder();
    }

    public static class FinTransactionBuilder {
        private FinTransactionType type;
        private LocalDate date;
        private Currency ccy;
        private BigDecimal netValue;
        private BigDecimal qty;
        private BigDecimal price;
        private Asset asset;
        private BigDecimal grossValue;
        private BigDecimal tax;
        private BigDecimal fee;
        private LocalDate settleDate;
        private String groupId;
        private String externalId;
        private String externalType;
        private String externalDetail;

        FinTransactionBuilder() {
        }

        public FinTransaction build() {
            return new FinTransaction(
                    this.type,
                    this.date,
                    this.ccy,
                    this.netValue,
                    this.qty,
                    this.price,
                    this.asset,
                    this.grossValue,
                    this.tax,
                    this.fee,
                    this.settleDate,
                    this.groupId,
                    this.externalId,
                    this.externalType,
                    this.externalDetail
            );
        }

        public FinTransactionBuilder reconcileNetValue() {
            netValue = requireNonNullElse(grossValue, ZERO).add(requireNonNullElse(fee, ZERO)).add(requireNonNullElse(tax, ZERO));
            return this;
        }

        public FinTransactionBuilder reconcileGrossValue() {
            grossValue = requireNonNullElse(netValue, ZERO).subtract(requireNonNullElse(fee, ZERO)).subtract(requireNonNullElse(tax, ZERO));
            return this;
        }

        public FinTransactionType type() {
            return type;
        }

        public FinTransactionBuilder type(FinTransactionType type) {
            this.type = type;
            return this;
        }

        public LocalDate date() {
            return date;
        }

        public FinTransactionBuilder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public Currency ccy() {
            return ccy;
        }

        public FinTransactionBuilder ccy(Currency ccy) {
            this.ccy = ccy;
            return this;
        }

        public BigDecimal netValue() {
            return netValue;
        }

        public FinTransactionBuilder netValue(BigDecimal netValue) {
            this.netValue = netValue;
            return this;
        }

        public BigDecimal qty() {
            return qty;
        }

        public FinTransactionBuilder qty(BigDecimal qty) {
            this.qty = qty;
            return this;
        }

        public BigDecimal price() {
            return price;
        }

        public FinTransactionBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Asset asset() {
            return asset;
        }

        public FinTransactionBuilder asset(Asset asset) {
            this.asset = asset;
            return this;
        }

        public BigDecimal grossValue() {
            return grossValue;
        }

        public FinTransactionBuilder grossValue(BigDecimal grossValue) {
            this.grossValue = grossValue;
            return this;
        }

        public BigDecimal tax() {
            return tax;
        }

        public FinTransactionBuilder tax(BigDecimal tax) {
            this.tax = tax;
            return this;
        }

        public BigDecimal fee() {
            return fee;
        }

        public FinTransactionBuilder fee(BigDecimal fee) {
            this.fee = fee;
            return this;
        }

        public LocalDate settleDate() {
            return settleDate;
        }

        public FinTransactionBuilder settleDate(LocalDate settleDate) {
            this.settleDate = settleDate;
            return this;
        }

        public String groupId() {
            return groupId;
        }

        public FinTransactionBuilder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public String externalId() {
            return externalId;
        }

        public FinTransactionBuilder externalId(String extraId) {
            this.externalId = extraId;
            return this;
        }

        public String externalType() {
            return externalType;
        }

        public FinTransactionBuilder externalType(String extraType) {
            this.externalType = extraType;
            return this;
        }

        public String externalDetail() {
            return externalDetail;
        }

        public FinTransactionBuilder externalDetail(String extraDetail) {
            this.externalDetail = extraDetail;
            return this;
        }
    }
}
