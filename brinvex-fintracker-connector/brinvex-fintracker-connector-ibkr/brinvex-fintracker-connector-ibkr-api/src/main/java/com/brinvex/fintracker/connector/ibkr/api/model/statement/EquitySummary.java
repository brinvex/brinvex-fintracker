package com.brinvex.fintracker.connector.ibkr.api.model.statement;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EquitySummary(
        LocalDate reportDate,
        String currency,
        BigDecimal cash,
        BigDecimal stock,
        BigDecimal dividendAccruals,
        BigDecimal interestAccruals,
        BigDecimal total
) implements Serializable {
}
