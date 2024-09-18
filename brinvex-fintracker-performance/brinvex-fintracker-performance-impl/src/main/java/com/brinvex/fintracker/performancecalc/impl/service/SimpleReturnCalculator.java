package com.brinvex.fintracker.performancecalc.impl.service;

import com.brinvex.fintracker.performancecalc.api.model.AnnualizationOption;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;

public class SimpleReturnCalculator {

    public static BigDecimal calculateSimpleReturn(
            BigDecimal startAssetValueExcl,
            BigDecimal endAssetValueIncl,
            LocalDate startDateIncl,
            LocalDate endDateIncl,
            AnnualizationOption annualizationOption,
            int calcScale,
            RoundingMode roundingMode
    ) {
        BigDecimal cumulReturn;
        if (startAssetValueExcl.compareTo(ZERO) == 0) {
            if (endAssetValueIncl.compareTo(ZERO) != 0) {
                throw new IllegalArgumentException((
                        "if startAssetValueExcl is zero, then the endAssetValueIncl must be zero, given: %s")
                        .formatted(startAssetValueExcl));
            }
            cumulReturn = ZERO;
        } else if (endAssetValueIncl.compareTo(ZERO) == 0) {
            cumulReturn = ONE.negate();
        } else {
            cumulReturn = endAssetValueIncl.subtract(startAssetValueExcl).divide(startAssetValueExcl, calcScale, roundingMode);
        }

        return AnnualizationUtil.annualizeReturn(annualizationOption, cumulReturn, startDateIncl, endDateIncl);
    }
}
