package com.brinvex.ptfactivity.connector.rvlt.api.model.statement;

import com.brinvex.finance.types.enu.Currency;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record Transaction(
        ZonedDateTime date,
        Currency ccy,
        String country,
        String symbol,
        TransactionType type,
        BigDecimal qty,
        BigDecimal price,
        TransactionSide side,
        BigDecimal value,
        String securityName,
        BigDecimal grossAmount,
        BigDecimal fees,
        BigDecimal commission,
        BigDecimal withholdingTax,
        String isin
) {
    public static TransactionBuilder builder() {
        return new TransactionBuilder();
    }

    public TransactionBuilder toBuilder() {
        return new TransactionBuilder()
                .date(this.date)
                .ccy(this.ccy)
                .country(this.country)
                .symbol(this.symbol)
                .type(this.type)
                .qty(this.qty)
                .price(this.price)
                .side(this.side)
                .value(this.value)
                .securityName(this.securityName)
                .grossAmount(this.grossAmount)
                .fees(this.fees)
                .commission(this.commission)
                .withholdingTax(this.withholdingTax)
                .isin(this.isin);
    }

    public static class TransactionBuilder {
        private ZonedDateTime date;
        private Currency ccy;
        private String country;
        private String symbol;
        private TransactionType type;
        private BigDecimal qty;
        private BigDecimal price;
        private TransactionSide side;
        private BigDecimal value;
        private String securityName;
        private BigDecimal grossAmount;
        private BigDecimal fees;
        private BigDecimal commission;
        private BigDecimal withholdingTax;
        private String isin;

        TransactionBuilder() {
        }

        public TransactionBuilder date(ZonedDateTime date) {
            this.date = date;
            return this;
        }

        public TransactionBuilder ccy(Currency ccy) {
            this.ccy = ccy;
            return this;
        }

        public TransactionBuilder country(String country) {
            this.country = country;
            return this;
        }

        public TransactionBuilder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public TransactionBuilder type(TransactionType type) {
            this.type = type;
            return this;
        }

        public TransactionBuilder qty(BigDecimal qty) {
            this.qty = qty;
            return this;
        }

        public TransactionBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public TransactionBuilder side(TransactionSide side) {
            this.side = side;
            return this;
        }

        public TransactionBuilder value(BigDecimal value) {
            this.value = value;
            return this;
        }

        public TransactionBuilder securityName(String securityName) {
            this.securityName = securityName;
            return this;
        }

        public TransactionBuilder grossAmount(BigDecimal grossAmount) {
            this.grossAmount = grossAmount;
            return this;
        }

        public TransactionBuilder fees(BigDecimal fees) {
            this.fees = fees;
            return this;
        }

        public TransactionBuilder commission(BigDecimal commission) {
            this.commission = commission;
            return this;
        }

        public TransactionBuilder withholdingTax(BigDecimal withholdingTax) {
            this.withholdingTax = withholdingTax;
            return this;
        }

        public TransactionBuilder isin(String isin) {
            this.isin = isin;
            return this;
        }

        public Transaction build() {
            return new Transaction(
                    this.date,
                    this.ccy,
                    this.country,
                    this.symbol,
                    this.type,
                    this.qty,
                    this.price,
                    this.side,
                    this.value,
                    this.securityName,
                    this.grossAmount,
                    this.fees,
                    this.commission,
                    this.withholdingTax,
                    this.isin);
        }
    }
}
