package com.brinvex.ptfactivity.connector.fiob.api.model.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @param rawCcy Represents the currency in which the transaction is denominated.
 *            For most transactions, this holds the ISO 4217 currency code (e.g., "USD", "EUR").
 *            However, in the case of a SPINOFF transaction, it holds the symbol
 *            of the SPINOFF child entity instead of a currency.
 */
public record TradingTransaction(
        LocalDateTime tradeDate,
        TradingTransactionDirection direction,
        String symbol,
        String rawSymbol,
        BigDecimal price,
        BigDecimal shares,
        String rawCcy,
        BigDecimal volumeCzk,
        BigDecimal feesCzk,
        BigDecimal volumeUsd,
        BigDecimal feesUsd,
        BigDecimal volumeEur,
        BigDecimal feesEur,
        String market,
        String instrumentName,
        LocalDate settleDate,
        String status,
        String orderId,
        String text,
        String userComments,
        int rowNumberOverStatementLine,
        int statementLineHash
) {

    public static TradingTransactionBuilder builder() {
        return new TradingTransactionBuilder();
    }

    public static class TradingTransactionBuilder {
        private LocalDateTime tradeDate;
        private TradingTransactionDirection direction;
        private String symbol;
        private String rawSymbol;
        private BigDecimal price;
        private BigDecimal shares;
        private String rawCcy;
        private BigDecimal volumeCzk;
        private BigDecimal feesCzk;
        private BigDecimal volumeUsd;
        private BigDecimal feesUsd;
        private BigDecimal volumeEur;
        private BigDecimal feesEur;
        private String market;
        private String instrumentName;
        private LocalDate settleDate;
        private String status;
        private String orderId;
        private String text;
        private String userComments;
        private int rowNumberOverStatementLine;
        private int statementLineHash;

        TradingTransactionBuilder() {
        }

        public TradingTransactionBuilder tradeDate(LocalDateTime tradeDate) {
            this.tradeDate = tradeDate;
            return this;
        }

        public TradingTransactionBuilder direction(TradingTransactionDirection direction) {
            this.direction = direction;
            return this;
        }

        public TradingTransactionBuilder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public TradingTransactionBuilder rawSymbol(String rawSymbol) {
            this.rawSymbol = rawSymbol;
            return this;
        }

        public TradingTransactionBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public TradingTransactionBuilder shares(BigDecimal shares) {
            this.shares = shares;
            return this;
        }

        public TradingTransactionBuilder rawCcy(String rawCcy) {
            this.rawCcy = rawCcy;
            return this;
        }

        public TradingTransactionBuilder volumeCzk(BigDecimal volumeCzk) {
            this.volumeCzk = volumeCzk;
            return this;
        }

        public TradingTransactionBuilder feesCzk(BigDecimal feesCzk) {
            this.feesCzk = feesCzk;
            return this;
        }

        public TradingTransactionBuilder volumeUsd(BigDecimal volumeUsd) {
            this.volumeUsd = volumeUsd;
            return this;
        }

        public TradingTransactionBuilder feesUsd(BigDecimal feesUsd) {
            this.feesUsd = feesUsd;
            return this;
        }

        public TradingTransactionBuilder volumeEur(BigDecimal volumeEur) {
            this.volumeEur = volumeEur;
            return this;
        }

        public TradingTransactionBuilder feesEur(BigDecimal feesEur) {
            this.feesEur = feesEur;
            return this;
        }

        public TradingTransactionBuilder market(String market) {
            this.market = market;
            return this;
        }

        public TradingTransactionBuilder instrumentName(String instrumentName) {
            this.instrumentName = instrumentName;
            return this;
        }

        public TradingTransactionBuilder settleDate(LocalDate settleDate) {
            this.settleDate = settleDate;
            return this;
        }

        public TradingTransactionBuilder status(String status) {
            this.status = status;
            return this;
        }

        public TradingTransactionBuilder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public TradingTransactionBuilder text(String text) {
            this.text = text;
            return this;
        }

        public TradingTransactionBuilder userComments(String userComments) {
            this.userComments = userComments;
            return this;
        }

        public TradingTransactionBuilder rowNumberOverStatementLine(int rowNumberOverStatementLine) {
            this.rowNumberOverStatementLine = rowNumberOverStatementLine;
            return this;
        }

        public TradingTransactionBuilder statementLineHash(int statementLineHash) {
            this.statementLineHash = statementLineHash;
            return this;
        }

        public TradingTransaction build() {
            return new TradingTransaction(
                    this.tradeDate,
                    this.direction,
                    this.symbol,
                    this.rawSymbol,
                    this.price,
                    this.shares,
                    this.rawCcy,
                    this.volumeCzk,
                    this.feesCzk,
                    this.volumeUsd,
                    this.feesUsd,
                    this.volumeEur,
                    this.feesEur,
                    this.market,
                    this.instrumentName,
                    this.settleDate,
                    this.status,
                    this.orderId,
                    this.text,
                    this.userComments,
                    this.rowNumberOverStatementLine,
                    this.statementLineHash);
        }
    }
}