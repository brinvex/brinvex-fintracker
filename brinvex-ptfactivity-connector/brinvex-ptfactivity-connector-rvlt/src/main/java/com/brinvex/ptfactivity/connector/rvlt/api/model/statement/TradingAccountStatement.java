package com.brinvex.ptfactivity.connector.rvlt.api.model.statement;

import com.brinvex.finance.types.enu.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TradingAccountStatement(
        String accountName,
        String accountNumber,
        LocalDate periodStartIncl,
        LocalDate periodEndIncl,
        Currency ccy,
        BigDecimal startStocksValue,
        BigDecimal startCashValue,
        BigDecimal startValue,
        BigDecimal endStocksValue,
        BigDecimal endCashValue,
        BigDecimal endValue,
        List<Transaction> transactions
) {
}
