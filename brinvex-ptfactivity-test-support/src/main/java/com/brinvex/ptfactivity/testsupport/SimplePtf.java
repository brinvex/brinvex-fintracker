package com.brinvex.ptfactivity.testsupport;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.core.api.domain.Asset;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Stream;

import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

/// `SimplePtf` is a basic portfolio management implementation
/// that tracks asset holdings and cash balances across multiple currencies.
///
/// This class is intended to be used for testing and prototyping portfolio tracking.
public class SimplePtf {

    public static class Holding {
        private final String country;
        private final String symbol;
        private BigDecimal qty;
        private final List<FinTransaction> transactions;

        public Holding(String country, String symbol, BigDecimal qty) {
            this(country, symbol, qty, null);
        }

        @JsonCreator
        public Holding(
                @JsonProperty("country") String country,
                @JsonProperty("symbol") String symbol,
                @JsonProperty("qty") BigDecimal qty,
                @JsonProperty("transactions") List<FinTransaction> transactions
        ) {
            this.country = country;
            this.symbol = symbol;
            this.qty = qty;
            this.transactions = transactions == null ? new ArrayList<>() : new ArrayList<>(transactions);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", "Holding[", "]")
                    .add("c=" + country)
                    .add("s=" + symbol)
                    .add("q=" + qty)
                    .toString();
        }

        public String getCountry() {
            return country;
        }

        public String getSymbol() {
            return symbol;
        }

        public BigDecimal getQty() {
            return qty;
        }
    }

    private final List<Holding> holdings;
    private final Map<Currency, BigDecimal> wallets;
    private final List<FinTransaction> transactions;

    @JsonCreator
    public SimplePtf(
            @JsonProperty("wallets") LinkedHashMap<Currency, BigDecimal> wallets,
            @JsonProperty("holdings") List<Holding> holdings,
            @JsonProperty("transactions") List<FinTransaction> transactions
    ) {
        this.wallets = wallets == null ? new LinkedHashMap<>() : new LinkedHashMap<>(wallets);
        this.holdings = holdings == null ? new ArrayList<>() : new ArrayList<>(holdings);
        this.transactions = transactions == null ? new ArrayList<>() : new ArrayList<>(transactions);
    }

    public SimplePtf(Collection<FinTransaction> finTransactions) {
        transactions = new ArrayList<>();
        wallets = new LinkedHashMap<>();
        holdings = new ArrayList<>();
        applyTransactions(finTransactions);
    }

    public Set<Currency> getCurrencies() {
        return unmodifiableSet(wallets.keySet());
    }

    public BigDecimal getCash(Currency ccy) {
        return wallets.getOrDefault(ccy, ZERO);
    }

    public BigDecimal getHoldingQty(String country, String symbol) {
        Holding holding = getHolding(country, symbol);
        return holding == null ? ZERO : holding.qty;
    }

    public List<FinTransaction> getHoldingTransactions(String country, String symbol) {
        Holding holding = getHolding(country, symbol);
        return holding == null ? emptyList() : unmodifiableList(holding.transactions);
    }

    public List<FinTransaction> getTransactions() {
        return unmodifiableList(transactions);
    }

    public int getHoldingsCount() {
        return holdings.size();
    }

    public Map<String, Set<String>> getCountrySymbols(boolean includeZeroPositions) {
        Stream<Holding> stream = holdings.stream();
        if (!includeZeroPositions) {
            stream = stream.filter(h -> h.qty.compareTo(ZERO) != 0);
        }
        return stream.collect(groupingBy(Holding::getCountry, mapping(Holding::getSymbol, toSet())));
    }

    public void applyTransaction(FinTransaction tran) {
        try {
            String tranExternalId = tran.externalId();
            FinTransaction duplTran = transactions
                    .stream()
                    .filter(t -> t.equals(tran) || tranExternalId != null && tranExternalId.equals(t.externalId()))
                    .findAny()
                    .orElse(null);
            if (duplTran != null) {
                throw new IllegalArgumentException("Transaction already applied: newTran=%s, duplTran=%s".formatted(tran, duplTran));
            }

            FinTransactionType tranType = tran.type();
            Asset asset = tran.asset();
            String country;
            String symbol;
            if (asset == null) {
                country = null;
                symbol = null;
            } else {
                country = asset.country();
                symbol = asset.symbol();
            }
            Currency ccy = tran.ccy();
            BigDecimal netValue = tran.netValue();
            BigDecimal qty = tran.qty();

            if (netValue != null && netValue.compareTo(ZERO) != 0) {
                updateCash(ccy, netValue);
            }

            if (qty.compareTo(ZERO) != 0) {
                if (tranType.equals(FinTransactionType.FX_BUY) || tranType.equals(FinTransactionType.FX_SELL)) {
                    updateCash(Currency.valueOf(symbol), qty);
                } else {
                    Holding holding = updateHolding(country, symbol, qty);
                    holding.transactions.add(tran);
                }
            }
            transactions.add(tran);
        } catch (Exception e) {
            throw new IllegalStateException("Exception while applying transaction: %s, h=%s, w=%s".formatted(tran, holdings, wallets), e);
        }
    }

    public void applyTransactions(Collection<FinTransaction> trans) {
        for (FinTransaction tran : trans) {
            applyTransaction(tran);
        }
    }

    private Holding getHolding(String country, String symbol) {
        return holdings
                .stream()
                .filter(h -> country.equals(h.country))
                .filter(h -> symbol.equals(h.symbol))
                .findAny()
                .orElse(null);
    }

    private Holding updateHolding(String country, String symbol, BigDecimal qtyToAdd) {
        Holding holding = getHolding(country, symbol);
        if (holding == null) {
            holding = new Holding(country, symbol, requireNonNull(qtyToAdd));
            holdings.add(holding);
        } else {
            holding.qty = holding.qty.add(qtyToAdd);
        }
        return holding;
    }

    private void updateCash(Currency ccy, BigDecimal moneyToAdd) {
        wallets.merge(ccy, moneyToAdd, BigDecimal::add);
    }
}
