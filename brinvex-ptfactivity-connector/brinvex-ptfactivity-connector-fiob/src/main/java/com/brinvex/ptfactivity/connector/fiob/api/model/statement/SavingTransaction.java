package com.brinvex.ptfactivity.connector.fiob.api.model.statement;

import com.brinvex.finance.types.enu.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingTransaction(
        String id,
        LocalDate date,
        BigDecimal volume,
        Currency ccy,
        String type
) {

    public static SavingTransactionBuilder builder() {
        return new SavingTransactionBuilder();
    }

    public static class SavingTransactionBuilder {
        private String id;
        private LocalDate date;
        private BigDecimal volume;
        private Currency ccy;
        private String type;

        SavingTransactionBuilder() {
        }

        public SavingTransactionBuilder id(String id) {
            this.id = id;
            return this;
        }

        public SavingTransactionBuilder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public SavingTransactionBuilder volume(BigDecimal volume) {
            this.volume = volume;
            return this;
        }

        public SavingTransactionBuilder ccy(Currency ccy) {
            this.ccy = ccy;
            return this;
        }

        public SavingTransactionBuilder type(String type) {
            this.type = type;
            return this;
        }

        public SavingTransaction build() {
            return new SavingTransaction(
                    this.id,
                    this.date,
                    this.volume,
                    this.ccy,
                    this.type
            );
        }

    }
}