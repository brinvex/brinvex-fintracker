package com.brinvex.ptfactivity.connector.fiob.internal.service.parser;

import com.brinvex.ptfactivity.connector.fiob.api.model.statement.Lang;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.TradingTransactionDirection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SuppressWarnings({"SameParameterValue", "SpellCheckingInspection"})
public class FiobParsingUtil {

    public static LocalDateTime toLocalDateTime(String s, DateTimeFormatter dtf) {
        return s == null || s.isBlank() ? null : LocalDateTime.parse(s, dtf);
    }

    public static LocalDateTime toFioDateTime(String s) {
        return toLocalDateTime(s, Lazy.FIOB_STATEMENT_DTF);
    }

    public static LocalDate toLocalDate(String s, DateTimeFormatter dtf) {
        return s == null || s.isBlank() ? null : LocalDate.parse(s, dtf);
    }

    public static LocalDate toFioDate(String s) {
        return toLocalDate(s, Lazy.FIOB_STATEMENT_DF);
    }

    public static BigDecimal toDecimal(String s) {
        return s == null || s.isBlank() ? null : new BigDecimal(s.replace(" ", "").replace(',', '.'));
    }

    public static TradingTransactionDirection toDirection(Lang lang, String direction) {
        if (direction == null || direction.isBlank()) {
            return null;
        }
        if (lang.equals(Lang.CZ)) {
            if (direction.equalsIgnoreCase(("Nákup"))) {
                return TradingTransactionDirection.BUY;
            }
            if (direction.equalsIgnoreCase(("Prodej"))) {
                return TradingTransactionDirection.SELL;
            }
            if (direction.equalsIgnoreCase("Bankovní převod")) {
                return TradingTransactionDirection.BANK_TRANSFER;
            }
            if (direction.equalsIgnoreCase("Převod mezi měnami")) {
                return TradingTransactionDirection.CURRENCY_CONVERSION;
            }
        }
        if (lang.equals(Lang.SK)) {
            if (direction.equalsIgnoreCase(("Nákup"))) {
                return TradingTransactionDirection.BUY;
            }
            if (direction.equalsIgnoreCase(("Predaj"))) {
                return TradingTransactionDirection.SELL;
            }
            if (direction.equalsIgnoreCase("Bankový prevod")) {
                return TradingTransactionDirection.BANK_TRANSFER;
            }
            if (direction.equalsIgnoreCase("Prevod mezi menami")) {
                return TradingTransactionDirection.CURRENCY_CONVERSION;
            }
        }
        if (lang.equals(Lang.EN)) {
            if (direction.equalsIgnoreCase(("Buy"))) {
                return TradingTransactionDirection.BUY;
            }
            if (direction.equalsIgnoreCase(("Sell"))) {
                return TradingTransactionDirection.SELL;
            }
            if (direction.equalsIgnoreCase("Bank transfer")) {
                return TradingTransactionDirection.BANK_TRANSFER;
            }
            if (direction.equalsIgnoreCase("Currency Conversion")) {
                return TradingTransactionDirection.CURRENCY_CONVERSION;
            }
        }
        throw new IllegalArgumentException("Unexpected %s value: '%s'".formatted(lang, direction));
    }

    private static class Lazy {
        private static final DateTimeFormatter FIOB_STATEMENT_DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        private static final DateTimeFormatter FIOB_STATEMENT_DF = DateTimeFormatter.ofPattern("d.M.yyyy");
    }

}
