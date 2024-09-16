package com.brinvex.fintracker.performancecalc.impl.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class CalcUtil {

    public static BigDecimal annualizeReturn(BigDecimal cumulativeReturn, LocalDate beginDateIncl, LocalDate endDateIncl) {
        return annualizeGrowthFactor(cumulativeReturn.add(BigDecimal.ONE), beginDateIncl, endDateIncl).subtract(BigDecimal.ONE);
    }

    public static BigDecimal annualizeGrowthFactor(BigDecimal cumulativeGrowthFactor, LocalDate beginDateIncl, LocalDate endDateIncl) {
        LocalDate endDateExcl = endDateIncl.plusDays(1);
        long fullYears = ChronoUnit.YEARS.between(beginDateIncl, endDateExcl);
        long days = ChronoUnit.DAYS.between(beginDateIncl.plusYears(fullYears), endDateExcl);

        double cumGrowthFactor = cumulativeGrowthFactor.doubleValue();
        double exponent = 1.0 / (fullYears + (days / 365.0));
        return BigDecimal.valueOf(Math.pow(cumGrowthFactor, exponent));
    }

}
