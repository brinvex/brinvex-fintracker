package com.brinvex.ptfactivity.connector.ibkr.api.model.statement;

import com.brinvex.finance.types.enu.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EquitySummary(
        LocalDate reportDate,
        Currency currency,
        BigDecimal cash,
        BigDecimal stock,
        BigDecimal dividendAccruals,
        BigDecimal interestAccruals,
        BigDecimal total
) {
}
