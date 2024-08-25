package com.brinvex.fintracker.common.test;

import com.brinvex.fintracker.api.exception.FinTransactionProcessingException;
import com.brinvex.fintracker.api.model.domain.Asset;
import com.brinvex.fintracker.api.model.domain.FinTransaction;
import com.brinvex.fintracker.api.model.domain.FinTransactionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

/**
 * {@code SimplePtf} is a basic portfolio management implementation
 * that tracks asset holdings and cash balances across multiple currencies.
 * <p>
 * This class is intended to be used for testing and prototyping portfolio tracking.
 */
public class SimplePtf {

    private static class SimpleHolding {
        private String country;
        private String symbol;
        private BigDecimal qty;
        private final List<FinTransaction> transactions = new ArrayList<>();
        @Override
        public String toString() {
            return new StringJoiner(", ", "SimpleHolding[", "]")
                    .add("c=" + country)
                    .add("s=" + symbol)
                    .add("q=" + qty)
                    .toString();
        }
    }

    private final List<SimpleHolding> holdings = new ArrayList<>();
    private final Map<String, BigDecimal> wallets = new LinkedHashMap<>();
    private final List<FinTransaction> transactions = new ArrayList<>();

    public SimplePtf() {
    }

    public SimplePtf(Collection<FinTransaction> finTransactions) {
        applyTransactions(finTransactions);
    }

    public Set<String> getCurrencies() {
        return unmodifiableSet(wallets.keySet());
    }

    public BigDecimal getCash(String ccy) {
        return wallets.getOrDefault(ccy, ZERO);
    }

    public BigDecimal getHoldingQty(String country, String symbol) {
        SimpleHolding holding = getHolding(country, symbol);
        return holding == null ? ZERO : holding.qty;
    }

    public List<FinTransaction> getHoldingTransactions(String country, String symbol) {
        SimpleHolding holding = getHolding(country, symbol);
        return holding == null ? emptyList() : unmodifiableList(holding.transactions);
    }

    public List<FinTransaction> getTransactions() {
        return unmodifiableList(transactions);
    }

    public int getHoldingsCount() {
        return holdings.size();
    }

    public void applyTransaction(FinTransaction tran) {
        try {
            String tranId = tran.id();
            String tranExtraId = tran.extraId();
            FinTransaction duplTran = transactions
                    .stream()
                    .filter(t -> t.equals(tran) || (tranId != null && tranId.equals(t.id()) || (tranExtraId != null && tranExtraId.equals(t.extraId()))))
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
            String ccy = tran.ccy();
            BigDecimal netValue = tran.netValue();
            BigDecimal qty = tran.qty();

            if (netValue != null && netValue.compareTo(ZERO) != 0) {
                updateCash(ccy, netValue);
            }

            if (qty.compareTo(ZERO) != 0) {
                if (tranType.equals(FinTransactionType.FX_BUY) || tranType.equals(FinTransactionType.FX_SELL)) {
                    updateCash(symbol, qty);
                } else {
                    SimpleHolding holding = updateHolding(country, symbol, qty);
                    holding.transactions.add(tran);
                }
            }
            transactions.add(tran);
        } catch (Exception e) {
            throw new FinTransactionProcessingException("Exception while applying transaction: %s, h=%s, w=%s".formatted(tran, holdings, wallets), e);
        }
    }

    public void applyTransactions(Collection<FinTransaction> trans) {
        for (FinTransaction tran : trans) {
            applyTransaction(tran);
        }
    }

    private SimpleHolding getHolding(String country, String symbol) {
        return holdings
                .stream()
                .filter(h -> country.equals(h.country))
                .filter(h -> symbol.equals(h.symbol))
                .findAny()
                .orElse(null);
    }

    private SimpleHolding updateHolding(String country, String symbol, BigDecimal qtyToAdd) {
        SimpleHolding holding = getHolding(country, symbol);
        if (holding == null) {
            holding = new SimpleHolding();
            holding.country = country;
            holding.symbol = symbol;
            holding.qty = requireNonNull(qtyToAdd);
            holdings.add(holding);
        } else {
            holding.qty = holding.qty.add(qtyToAdd);
        }
        return holding;
    }

    private void updateCash(String ccy, BigDecimal moneyToAdd) {
        wallets.merge(ccy, moneyToAdd, BigDecimal::add);
    }

}
