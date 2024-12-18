package com.brinvex.ptfactivity.connector.fiob.internal.service.parser;

import com.brinvex.ptfactivity.connector.fiob.api.model.statement.Lang;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.TradingTransaction.TradingTransactionBuilder;
import com.brinvex.java.StringUtil;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;


@SuppressWarnings("SpellCheckingInspection")
public enum TradingTransColumnParser {

    TRADE_DATE("Dátum obchodu", "Datum obchodu", "Trade Date", FiobParsingUtil::toFioDateTime, TradingTransactionBuilder::tradeDate),
    DIRECTION("Smer", "Směr", "Direction", FiobParsingUtil::toDirection, TradingTransactionBuilder::direction),
    SYMBOL("Symbol", "Symbol", "Symbol", StringUtil::stripToNull, TradingTransColumnParser::fillSymbol),
    PRICE("Cena", "Cena", "Price", FiobParsingUtil::toDecimal, TradingTransactionBuilder::price),
    SHARES("Počet", "Počet", "Shares", FiobParsingUtil::toDecimal, TradingTransactionBuilder::shares),
    CURRENCY("Mena", "Měna", "Currency", StringUtil::stripToNull, TradingTransactionBuilder::rawCcy),
    VOLUME_CZK("Objem v CZK", "Objem v CZK", "Volume (CZK)", FiobParsingUtil::toDecimal, TradingTransactionBuilder::volumeCzk),
    FEES_CZK("Poplatky v CZK", "Poplatky v CZK", "Fees", FiobParsingUtil::toDecimal, TradingTransactionBuilder::feesCzk),
    VOLUME_USD("Objem v USD", "Objem v USD", "Volume in USD", FiobParsingUtil::toDecimal, TradingTransactionBuilder::volumeUsd),
    FEES_USD("Poplatky v USD", "Poplatky v USD", "Fees (USD)", FiobParsingUtil::toDecimal, TradingTransactionBuilder::feesUsd),
    VOLUME_EUR("Objem v EUR", "Objem v EUR", "Volume (EUR)", FiobParsingUtil::toDecimal, TradingTransactionBuilder::volumeEur),
    FEES_EUR("Poplatky v EUR", "Poplatky v EUR", "Fees (EUR)", FiobParsingUtil::toDecimal, TradingTransactionBuilder::feesEur),
    MARKET("Trh", "Trh", "Market", StringUtil::stripToNull, TradingTransactionBuilder::market),
    INSTRUMENT_NAME("Názov FN", "Název CP", "Title", StringUtil::stripToNull, TradingTransactionBuilder::instrumentName),
    SETTLEMENT_DATE("Dátum vysporiadania", "Datum vypořádání", "Settlement Date", FiobParsingUtil::toFioDate, TradingTransactionBuilder::settleDate),
    STATUS("Stav", "Stav", "Status", StringUtil::stripToNull, TradingTransactionBuilder::status),
    ORDER_ID("Pokyn ID", "Pokyn ID", "Order ID", StringUtil::stripToNull, TradingTransactionBuilder::orderId),
    TEXT("Text FIO", "Text FIO", "Text FIO", StringUtil::stripToNull, TradingTransactionBuilder::text),
    USER_COMMENTS("Užívateľská identifikácia", "Uživatelská identifikace", "User Comments", StringUtil::stripToNull, TradingTransactionBuilder::userComments),
    ;

    public static final Set<TradingTransColumnParser> STANDARD_COLUMNS = Set.of(
            TRADE_DATE,
            DIRECTION,
            SYMBOL,
            PRICE,
            SHARES,
            CURRENCY,
            VOLUME_CZK,
            FEES_CZK,
            VOLUME_USD,
            FEES_USD,
            VOLUME_EUR,
            FEES_EUR,
            TEXT
    );

    private final String titleSK;

    private final String titleCZ;

    private final String titleEN;

    private final BiFunction<Lang, String, ?> mapper;

    private final BiConsumer<TradingTransactionBuilder, ?> filler;

    <T> TradingTransColumnParser(
            String titleSK,
            String titleCZ,
            String titleEN,
            BiFunction<Lang, String, T> mapper,
            BiConsumer<TradingTransactionBuilder, T> filler
    ) {
        this.titleSK = titleSK;
        this.titleCZ = titleCZ;
        this.titleEN = titleEN;
        this.mapper = mapper;
        this.filler = filler;
    }

    @SuppressWarnings("unused")
    <T> TradingTransColumnParser(
            String titleSK,
            String titleCZ,
            String titleEN,
            Function<String, T> mapper,
            BiConsumer<TradingTransactionBuilder, T> filler
    ) {
        this(titleSK, titleCZ, titleEN, (lang, s) -> mapper.apply(s), filler);
    }

    public static TradingTransColumnParser ofTitle(String title, Lang lang) {
        for (TradingTransColumnParser value : values()) {
            if (value.getTitle(lang).equals(title)) {
                return value;
            }
        }
        return null;
    }

    public String getTitle(Lang lang) {
        return switch (lang) {
            case CZ -> titleCZ;
            case EN -> titleEN;
            case SK -> titleSK;
        };
    }

    private static void fillSymbol(TradingTransactionBuilder tranBuilder, String rawSymbol) {
        if (rawSymbol != null && !rawSymbol.isBlank()) {
            tranBuilder.rawSymbol(rawSymbol);
            tranBuilder.symbol(rawSymbol.endsWith("*") ? rawSymbol.substring(0, rawSymbol.length() - 1) : rawSymbol);
        }
    }

    public void fill(TradingTransactionBuilder tranBuilder, String cell, Lang lang) {
        Object value = mapper.apply(lang, cell);
        @SuppressWarnings("unchecked")
        BiConsumer<TradingTransactionBuilder, Object> filler = (BiConsumer<TradingTransactionBuilder, Object>) this.filler;
        filler.accept(tranBuilder, value);
    }
}
