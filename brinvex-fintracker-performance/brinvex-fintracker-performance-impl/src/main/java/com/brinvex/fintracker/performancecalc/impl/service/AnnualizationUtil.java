package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class AnnualizationUtil {

    public static BigDecimal annualizeReturn(
            AnnualizationOption annualizationOption,
            BigDecimal cumulativeReturn,
            LocalDate startDateIncl,
            LocalDate endDateIncl
    ) {
        BigDecimal cumulFactor = cumulativeReturn.add(BigDecimal.ONE);
        BigDecimal annFactor = annualizeGrowthFactor(annualizationOption, cumulFactor, startDateIncl, endDateIncl);
        return annFactor.subtract(BigDecimal.ONE);
    }

    public static BigDecimal annualizeGrowthFactor(
            AnnualizationOption annualizationOption,
            BigDecimal cumulativeGrowthFactor,
            LocalDate startDateIncl,
            LocalDate endDateIncl
    ) {
        if (annualizationOption == AnnualizationOption.DO_NOT_ANNUALIZE) {
            return cumulativeGrowthFactor;
        }
        if (cumulativeGrowthFactor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        LocalDate endDateExcl = endDateIncl.plusDays(1);
        long fullYears = ChronoUnit.YEARS.between(startDateIncl, endDateExcl);
        long days = ChronoUnit.DAYS.between(startDateIncl.plusYears(fullYears), endDateExcl);
        if (annualizationOption == AnnualizationOption.ANNUALIZE_IF_OVER_ONE_YEAR) {
            if (fullYears == 0 || (fullYears == 1 && days == 0)) {
                return cumulativeGrowthFactor;
            }
        }
        double cumGrowthFactor = cumulativeGrowthFactor.doubleValue();
        double exponent = 1.0 / (fullYears + (days / 365.0));
        return BigDecimal.valueOf(Math.pow(cumGrowthFactor, exponent));
    }

}
