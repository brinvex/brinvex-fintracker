package com.brinvex.ptfactivity.connector.amnd.api.model.statement;

import com.brinvex.finance.types.enu.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Trade(
        TradeType type,
        String accountId,
        LocalDate orderDate,
        LocalDate tradeDate,
        BigDecimal netValue,
        BigDecimal fee,
        BigDecimal qty,
        BigDecimal price,
        LocalDate priceDate,
        Currency ccy,
        String isin,
        String instrumentName,
        String description,
        LocalDate settleDate
) {
    public static TradeBuilder builder() {
        return new TradeBuilder();
    }

    public static class TradeBuilder {
        private TradeType type;
        private String accountId;
        private LocalDate orderDate;
        private LocalDate tradeDate;
        private BigDecimal netValue;
        private BigDecimal fee;
        private BigDecimal qty;
        private BigDecimal price;
        private LocalDate priceDate;
        private Currency ccy;
        private String isin;
        private String instrumentName;
        private String description;
        private LocalDate settleDate;

        TradeBuilder() {
        }

        public TradeBuilder type(TradeType type) {
            this.type = type;
            return this;
        }

        public TradeBuilder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public TradeBuilder orderDate(LocalDate orderDate) {
            this.orderDate = orderDate;
            return this;
        }

        public TradeBuilder tradeDate(LocalDate tradeDate) {
            this.tradeDate = tradeDate;
            return this;
        }

        public TradeBuilder netValue(BigDecimal netValue) {
            this.netValue = netValue;
            return this;
        }

        public TradeBuilder fee(BigDecimal fee) {
            this.fee = fee;
            return this;
        }

        public TradeBuilder qty(BigDecimal qty) {
            this.qty = qty;
            return this;
        }

        public TradeBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public TradeBuilder priceDate(LocalDate priceDate) {
            this.priceDate = priceDate;
            return this;
        }

        public TradeBuilder ccy(Currency ccy) {
            this.ccy = ccy;
            return this;
        }

        public TradeBuilder isin(String isin) {
            this.isin = isin;
            return this;
        }

        public TradeBuilder instrumentName(String instrumentName) {
            this.instrumentName = instrumentName;
            return this;
        }

        public TradeBuilder description(String description) {
            this.description = description;
            return this;
        }

        public TradeBuilder settleDate(LocalDate settleDate) {
            this.settleDate = settleDate;
            return this;
        }

        public Trade build() {
            return new Trade(
                    this.type,
                    this.accountId,
                    this.orderDate,
                    this.tradeDate,
                    this.netValue,
                    this.fee,
                    this.qty,
                    this.price,
                    this.priceDate,
                    this.ccy,
                    this.isin,
                    this.instrumentName,
                    this.description,
                    this.settleDate
            );
        }
    }
}
